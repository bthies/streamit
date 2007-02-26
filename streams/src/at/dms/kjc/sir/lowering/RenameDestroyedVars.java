package at.dms.kjc.sir.lowering;

import java.util.*;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;

// Given a filter and a set of variables created by ArrayDestroyer rename
// and reduce number of the variables by analyzing live ranges.

// If variable is first assigned in a nest of ForStatements, assume its
// live range starts at the beginning of the outmost ForStatement

// If variable is last used in a nest of ForStatements, assume its
// live range ends at the end of the outmost ForStatement


public class RenameDestroyedVars extends SLIRReplacingVisitor {

    private Set destroyedVars;
    
    private Random random;

    private RenameDestroyedVars(Set vars) {
        destroyedVars = vars;
        random = KjcOptions.fixseed? new Random(17): new Random();
    }

    private HashMap<CType, Integer> live_vars; // CType -> Integer
    private HashMap<CType, Integer> max_live_vars; // CType -> Integer
    
    private boolean RENAME = false;
    private boolean assign = false;

    // first assign expression of a destroyed variable 
    // JLocalVariable -> JLocalVariableExpression or JForStatement
    private HashMap<JLocalVariable, JPhylum> first_assign; 
    
    // last usage expresion of a destroyed varibale
    // JLocalVariable -> JLocalVariableExpression or JForStatement
    private HashMap<JLocalVariable, JPhylum> last_usage;   

    // JForStatement -> LinkedList of JLocalVariables
    private HashMap<JForStatement, LinkedList> first_assign_for_loop;

    // JForStatement -> LinkedList of JLocalVariables
    private HashMap<JForStatement, LinkedList> last_usage_for_loop;
    
    private HashMap<JLocalVariable, JVariableDefinition> var_alias; // assigned alias of variable Var -> New var
    private HashMap<CType, Stack> available_names; // type -> Stack (stack of available names for a type)
    private HashMap<CType, LinkedList> renamed_vars; // type -> LinkedList

    private LinkedList<JForStatement> for_stmts; 

    public static void renameDestroyedVars(SIRFilter filter, Set destroyed_vars) {

        RenameDestroyedVars rename = new RenameDestroyedVars(destroyed_vars);

        JMethodDeclaration methods[] = filter.getMethods();
        for (int i = 0; i < methods.length; i++) {

            rename.for_stmts = new LinkedList<JForStatement>();
            rename.first_assign = new HashMap<JLocalVariable, JPhylum>();
            rename.last_usage = new HashMap<JLocalVariable, JPhylum>();
            rename.first_assign_for_loop = new HashMap<JForStatement, LinkedList>();
            rename.last_usage_for_loop = new HashMap<JForStatement, LinkedList>();

            rename.RENAME = false;
            methods[i].accept(rename);

            rename.RENAME = true;

            rename.live_vars = new HashMap<CType, Integer>();
            rename.max_live_vars = new HashMap<CType, Integer>();
            rename.available_names = new HashMap<CType, Stack>();
            rename.renamed_vars = new HashMap<CType, LinkedList>();
            rename.var_alias = new HashMap<JLocalVariable, JVariableDefinition>();

            // initalize first_assign_for_loop and
            // last_usage_for_loop hash maps.
            rename.init_for_hash_maps();
        
            methods[i].accept(rename);

            Set<CType> types = rename.max_live_vars.keySet();
            Iterator<CType> iter = types.iterator();

            while (iter.hasNext()) {
                Object type = iter.next();
                int num = rename.max_live_vars.get(type).intValue();
                System.out.println("[Function: "+methods[i].getName()+" Type: "+type+" Max-live-destroyed-vars: "+num+"]");

                JBlock body = methods[i].getBody();
                Stack alias_stack = rename.available_names.get(type);
                JVariableDefinition var;
        
                LinkedList vars = rename.renamed_vars.get(type);
                ListIterator li = vars.listIterator();

                for (int y = 0; y < num; y++) {
                    //assert (!alias_stack.empty());
                    //var = (JVariableDefinition)alias_stack.pop();

                    assert(li.hasNext());
                    var = (JVariableDefinition)li.next();
                    body.addStatementFirst(new JVariableDeclarationStatement(null, var, null));
                }
            }
        }
    }


    // initalize first_assign_for_loop and
    // last_usage_for_loop hash maps.

    private void init_for_hash_maps() {

        Set<JLocalVariable> keySet = first_assign.keySet();
        Iterator<JLocalVariable> iter = keySet.iterator();
        while (iter.hasNext()) {
            JLocalVariable var = iter.next();
            Object obj = first_assign.get(var);

            if (obj instanceof JForStatement) {
                JForStatement for_stmt = (JForStatement)obj;
                if (!first_assign_for_loop.containsKey(for_stmt)) {
                    first_assign_for_loop.put(for_stmt, new LinkedList());
                }
                LinkedList<JLocalVariable> list = first_assign_for_loop.get(for_stmt);
                list.addLast(var);
            }
        }
        
        keySet = last_usage.keySet();
        iter = keySet.iterator();
        while (iter.hasNext()) {
            JLocalVariable var = iter.next();
            Object obj = last_usage.get(var);

            if (obj instanceof JForStatement) {
                JForStatement for_stmt = (JForStatement)obj;
                if (!last_usage_for_loop.containsKey(for_stmt)) {
                    last_usage_for_loop.put(for_stmt, new LinkedList());
                }
                LinkedList<JLocalVariable> list = last_usage_for_loop.get(for_stmt);
                list.addLast(var);
            }
        }
    }

    public Object visitForStatement(JForStatement self,
                                    JStatement init,
                                    JExpression cond,
                                    JStatement incr,
                                    JStatement body) {
        Object result;
        for_stmts.addLast(self); // add for loop to the stack of for statements

        if (RENAME) {
            if (first_assign_for_loop.containsKey(self)) {
                LinkedList list = first_assign_for_loop.get(self);
                ListIterator li = list.listIterator();
                while (li.hasNext()) {
                    JLocalVariable var = (JLocalVariable)li.next();
                    liveRangeStart(var);
                }
            }
        }

        result = super.visitForStatement(self,init,cond,incr,body);
        assert (for_stmts.removeLast() == self); // make sure we remove self from stack

        if (RENAME) {
            if (last_usage_for_loop.containsKey(self)) {
                LinkedList list = last_usage_for_loop.get(self);
                ListIterator li = list.listIterator();
                while (li.hasNext()) {
                    JLocalVariable var = (JLocalVariable)li.next();
                    liveRangeEnd(var);
                }
            }
        }

        return result;
    }


    public Object visitAssignmentExpression(JAssignmentExpression self,
                                            JExpression left,
                                            JExpression right) {

        assign = false;
        JExpression newExp = (JExpression)right.accept(this);
        if (newExp!=null && newExp!=right) { self.setRight(newExp); }

        assign = false;
        if (left instanceof JLocalVariableExpression) { assign = true; }
        newExp = (JExpression)left.accept(this);
        if (newExp!=null && newExp!=left) { self.setLeft(newExp); }
        assign = false; // must set assign to false!!

        return self;
    }

    public Object visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
                                                    int oper,
                                                    JExpression left,
                                                    JExpression right) {

        assign = false;
        JExpression newExp = (JExpression)right.accept(this);
        if (newExp!=null && newExp!=right) { self.setRight(newExp); }

        assign = false;
        if (left instanceof JLocalVariableExpression) { assign = true; }
        newExp = (JExpression)left.accept(this);
        if (newExp!=null && newExp!=left) { self.setLeft(newExp); }
        assign = false; // must set assign to false!!

        return self;
    }


    public Object visitVariableDeclarationStatement(JVariableDeclarationStatement self,
                                                    JVariableDefinition[] vars) {

        if (!RENAME) return self;

        // We are in rename mode, eliminate destroyed variables

        ArrayList<JVariableDefinition> newVars = new ArrayList<JVariableDefinition>();
        for (int i = 0; i < vars.length; i++) {
            if (!destroyedVars.contains(vars[i])) newVars.add(vars[i]);
        }

        if (newVars.size()>0) {
            // if we have some vars, adjust us
            self.setVars(newVars.toArray(new JVariableDefinition[0]));
            return self;
        } else {
            // otherwise, replace us with empty statement
            return new JEmptyStatement(null, null);
        }
    }

    public Object visitLocalVariableExpression(JLocalVariableExpression self,
                                               String ident) {
        JLocalVariable var = self.getVariable();
        if (destroyedVars.contains(var)) {
            if (!RENAME) {

                // discover first assign and last use!
                if (assign) {
                    //System.out.println("destroyed variable: "+ident+" assigned to!");
                    if (!first_assign.containsKey(var)) {

                        if (for_stmts.size() == 0) {
                            first_assign.put(var,self);
                        } else {

                            //System.out.println("Destroyed variable "+var.getIdent()+" first assigned in a for loop!");
                            first_assign.put(var,for_stmts.getFirst());
                        }
                    }
                } else {
                    //System.out.println("destroyed variable: "+ident+" used.");

                    if (for_stmts.size() == 0) {
                        last_usage.put(var,self);
                    } else {

                        //System.out.println("Destroyed variable "+var.getIdent()+" used in a for loop!");
                        last_usage.put(var,for_stmts.getFirst());
                    }
                }

            } else {

                // rename variables and find max number of live variables of each type

                Object o1 = first_assign.get(self.getVariable());
                Object o2 = last_usage.get(self.getVariable());

                if (self == o1) {

                    liveRangeStart(var);

                    if (o2 == null) {
                        // variable is never used so we can reuse its alias
                        liveRangeEnd(var);
                    }
                }

                if (self == o2) {
                    liveRangeEnd(var);
                }

                JVariableDefinition defn = var_alias.get(self.getVariable());
                return new JLocalVariableExpression(null, defn);
            }
        }

        return self;
    }

    private void liveRangeStart(JLocalVariable var) {
    
        CType type = var.getType();

        // if we see the type first time create objects
        if (!live_vars.containsKey(type)) live_vars.put(type, new Integer(0));
        if (!max_live_vars.containsKey(type)) max_live_vars.put(type, new Integer(0));
        if (!available_names.containsKey(type)) available_names.put(type, new Stack());
        if (!renamed_vars.containsKey(type)) renamed_vars.put(type, new LinkedList());

        JVariableDefinition alias;

        // increase number of live variables
        int live = live_vars.get(type).intValue() + 1;
        live_vars.put(type, new Integer(live));

        if (live > max_live_vars.get(type).intValue()) {
    
            // if this is biggest number of live variables we have 
            // seen so far then create a new variable
            max_live_vars.put(type, new Integer(live));
            alias = new JVariableDefinition(null, 0, type, 
                                            "__destroyed_"+type.toString()+"_"+live, null);

            // add renamed variable to the linked list (into random position)
            LinkedList<JVariableDefinition> vars = renamed_vars.get(type);
            int index = random.nextInt()%(vars.size()+1);
            if (index < 0) index = -index;
            vars.add(index,alias); 

        } else {

            // get a free variable from the available variable stack
            Stack alias_stack = available_names.get(type);
            assert (!alias_stack.empty());
            alias = (JVariableDefinition)alias_stack.pop();
        }

        // save the alias of the variable
        var_alias.put(var, alias);
    }

    private void liveRangeEnd(JLocalVariable var) {

        CType type = var.getType();
    
        assert (live_vars.containsKey(type));
        int live = live_vars.get(type).intValue() - 1;
    
        assert (live >= 0);
        live_vars.put(type, new Integer(live));
    
        Stack<JVariableDefinition> alias_stack = available_names.get(type);
        alias_stack.push(var_alias.get(var));   
    }
}



