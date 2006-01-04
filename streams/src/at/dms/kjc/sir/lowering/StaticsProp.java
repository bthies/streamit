/**
 * 
 */
package at.dms.kjc.sir.lowering;

import java.util.*;
import java.lang.IllegalArgumentException;
import at.dms.kjc.*;
import at.dms.kjc.common.CommonUtils;
import at.dms.kjc.sir.*;
//import at.dms.kjc.iterator.SIRFilterIter;
import at.dms.kjc.sir.SIRToStreamIt;
import at.dms.kjc.sir.lowering.ConstantProp;
//import at.dms.kjc.sir.lowering.FieldProp;
import at.dms.kjc.iterator.*;

/**
 * StaticsProp.propagate propagates constants from 'static' sections.
 * 
 * StaticsProp.propagate does all the work.
 * 
 * This pass is expected to be run after lowering.RenameAll but
 * before any other lowering pass.
 * 
 * @author Allyn Dimock
 *
 *  This phase is currently written under the simplfying assumption that
 * every static section can be unwound to be assignments of constants to 
 * fields -- so no field depends on the value of any other field.  If this
 * assumption does not hold, then we need to create a joint back-slice of 
 * the static code for each filter that we are propagating into.
 *
 */
public class StaticsProp {
    
    static boolean debugPrint = false;
    
    // -------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------
    
    private StaticsProp(Set/*<SIRGlobal>*/ theStatics) {
        
        // during processing we will want 
        //   the static sections.
        //   their names,
        //   and a map from names to sections.
        
        HashSet/*<String>*/ staticsNames = new HashSet();
        HashMap/*<String, TheGlobal>*/ namesToStatics = new HashMap();
        
        for (Iterator/*SIRGlobal*/ i = theStatics.iterator(); i.hasNext(); ) {
            SIRGlobal aStatic = (SIRGlobal)i.next();
            String sName = aStatic.getIdent();
            staticsNames.add(sName);
            namesToStatics.put(sName, aStatic);
        }

//        this.theStatics = theStatics;
        this.staticsNames = staticsNames;
        this.namesToStatics = namesToStatics;
    }

    // -------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------
    
    // Names of all static sectoins.
    private Set/*<String>*/ staticsNames;

    // map from name of static section to the section
    private Map/*<String, TheGlobal>*/ namesToStatics;

    // map from the name of a SIRStream element to all
    // static fields that it uses (by static section name, field name)
    private Map/*<String,Set<StaticAndField>>*/ streamIdentToField = new HashMap();

    // the current SIRStream element name for iterators. (Allows me to 
    // use the value in code in inner classes without having to define them
    // inside the method that uses them...
    private String currentStreamIdent;
    
    // map from a static section name and field name to the code for
    // declaring (and statically initializing) the field.
    private Map/*<StaticAndField,JFieldDeclaration>*/ staticNameToFields = new HashMap();
 
    // map from a static section name and a field name to the code that
    // is needed to assign value(s) to the field.
    private Map/*<StaticAndField,List<JStatement>>*/ staticNameToAssignments = new HashMap();

    // map from a static section and field name to the set of synonyms for
    // the field.
    private Map/*<StaticAndField,Set<String>>*/ staticNameToNewNames = new HashMap();

    // map from each propagated field name to a set
    // of synonyms (including the name itself)
    private Map/*<String,Set<String>>*/ nameToName = new HashMap();

    
    // for conversion to local variables, there must be a single definition of the variable.
    private final Map/*<String,JVariavleDefinition>*/ identToVarDefn = new HashMap();

    /*
     * I thought to eventually support multiple static sections:
     * So I have to associate a SIRStream with each combination
     * of static section and field that it references.
     * This necessitates writing a simple pair structure that
     * will hash well.
     */
    private static class StaticAndField {
        private String theStatic;
        private String theField;
        
        StaticAndField(String theStatic, String theField) {
            if (theStatic == null || theField == null) {
                throw new IllegalArgumentException();
            }
            this.theStatic = theStatic;
            this.theField = theField;
        }
        
        public String getTheField() { return theField; }
        public String getTheStatic() { return theStatic; }
        
        public boolean equals(Object o) {
            if (o.getClass().equals(this.getClass())) {
                StaticAndField s = (StaticAndField)o;
                return theStatic.equals(s.theStatic) 
                    && theField.equals(s.theField);
            } else return false;
        }
        
        public int hashCode() {
            return theStatic.hashCode() ^ theField.hashCode();
        }
        
        // used in debugging only. There is no comitment to this format!
        public String toString() { return theStatic + "." + theField; }
    }


    // --------------------------------------------------------------------
    // Methods supporting top-level use
    // --------------------------------------------------------------------
    
    /**
     * The top-level for propagating data from static sections into the 
     * stream graph.
     * 
     * @param str  The stream program
     * @param theStatics a set of static sections that need to be propagated
     *        through the stream.
     *
     * @return a map of associations between propagated data.  This
     *         information may be useful in a fusion: if two filters
     *         with associated data are fused, only one copy is needed.
     *         
     *         Of course, for some static data -- using a static section
     *         to set parameters, or push, peek, or pop rates -- it is
     *         necessary that the static data be constants that can be
     *         propagated to place with FieldProp in the streams.
     *  
     */
    public static Map propagate(SIRStream str, Set/*<SIRGlobal>*/ theStatics) {
        linearizeStatics(theStatics);

        if (debugPrint) {
            System.err.println("// StaticsProp: globals after linearization");
            for (Iterator it = theStatics.iterator(); it.hasNext();) {
                SIRToStreamIt.runOnGlobal((SIRGlobal) it.next());
            }
            System.err
                    .println("// StaticsProp: End of globals after linearization");
        }
        StaticsProp sp = new StaticsProp(theStatics);
        sp.findStaticsForStr(str);
        if (debugPrint) {System.err.println(sp.streamIdentToField);}
        sp.getCodeToPropagate();
        sp.propagateIntoStreams(str);
        return sp.nameToName;
    }
    
    /**
     * Turn each static section into linear code.
     * 
     * At the end of this, code in a static section _should_
     * only depend on constants, there should be no interdependencies.
     * TODO: write checker for this property since if not correct
     * the propagated lines from statics will create errors in other filters
     *
     * @param theStatics  list of static sections
     */
    private static void linearizeStatics(Set/*<SIRGlobal>*/ theStatics) {
        // Temporarily turn off structure elimination since
        // it should not have been run on the stream yet.
        boolean oldStructs = KjcOptions.struct;
        KjcOptions.struct = false;
        for (Iterator i = theStatics.iterator(); i.hasNext();) {
            SIRGlobal s = (SIRGlobal) i.next();
            // hopefully, this sequence will evaluate any code
            // in the init portion of the static block and update
            // fields to be constant. FieldProp calls a subset of
            // ConstantProp again. FieldProp does not iterate to a fixed
            // point...
            
            ConstantProp.propagateAndUnroll(s, false);
            // Propagate fields and unroll outer (for) loops
            //FieldProp.doPropagate(s, true, false);

            // Propagate fields again since FieldProp does not iterate to
            // a fixed point.
            //FieldProp.doPropagate(s, false, false);

            // At this point there is no guarantee that any static
            // section is completely unrolled.  TODO: we really need
            // to know what static fields need to be constants and
            // check that they become constants.  TODO: if static
            // fields do not need to become constants, we should be
            // propagating back-slices of the fields rather than only
            // the field definitions and initializations.
        }
        KjcOptions.struct = oldStructs;
    }

    // --------------------------------------------------------------------
    // Visitor for associating Streams with fields from static sections
    // --------------------------------------------------------------------
    
    /*
     * Given a SIRStream, find all static fields referred to in the stream
     * and any other reachable streams, and record te association between
     * each stream and the static fields that it references.
     * 
     * Record them in this.streamIdentToField
     */
    private void findStaticsForStr(SIRStream str) {

        MyIter myIter = new MyIter();
        myIter.iterOverAllFieldsAndMethods (str, true, true, 
                new FindStaticsInStr());
    }
    // Specialize stream walker to maintain this.currentStreamIdent
    // so that the FindStaticsInStr visitor can record stream names
    class MyIter extends IterOverAllFieldsAndMethods {
	protected boolean preVisit(SIRStream str) {
	    currentStreamIdent = str.getIdent();
	    return true;
	}
    };
    
    /*
     * Given a Kjc / SLIR expression, find all static fields referenced
     * and store them in streamIdentToField
     */
    private class FindStaticsInStr extends SLIREmptyVisitor {
        public void visitFieldExpression(JFieldAccessExpression self,
                JExpression left, String fieldIdent) {
            super.visitFieldExpression(self,left,fieldIdent);
            if (left instanceof JTypeNameExpression) {
                String className = ((JTypeNameExpression)left).
                    getType().toString();
                if (staticsNames.contains(className)) {
                    addToMapValueSet(streamIdentToField,currentStreamIdent,
                                    new StaticAndField(className, fieldIdent));
                }
            }
        }
    }

    
    // --------------------------------------------------------------------
    // Utlity phase: for each piece of code that needs propagation, pull
    // code out of the static section to (1) put into field definitions,
    // and (2) code to put into init functions.
    //
    // This phase is written under the simplfying assumption that
    // every static section has had its code unwound to be constants so that
    // no field depends on the value of any other field.  If this assumption
    // does not hold, then we need to create a joint back-slice of the static
    // code for each filter that we are propagating into.
    // --------------------------------------------------------------------
    
    private void getCodeToPropagate() {
        Set/*<StaticAndField>*/ usedStaticAndFields = new HashSet();
        // from map key->Set<StaticAndField>  to Set<StaticAndField>
        for (Iterator iter = streamIdentToField.values().iterator(); iter.hasNext();) {
            usedStaticAndFields.addAll((Set)iter.next());
         }
        
        for (Iterator iter = usedStaticAndFields.iterator(); iter.hasNext();) {
            StaticAndField sNameAndField = (StaticAndField)iter.next();
            String thisStaticName = sNameAndField.getTheStatic();
            String thisFieldName = sNameAndField.getTheField();
            SIRGlobal thisStatic = (SIRGlobal)namesToStatics.get(thisStaticName);
            
            staticNameToFields.put(sNameAndField,
                    getFieldDecl(thisStatic, thisFieldName));
            staticNameToAssignments.put(sNameAndField,
                    getFieldAssignments(thisStatic, thisFieldName));
        }
        if (debugPrint) {
            System.err.println(staticNameToFields);
            System.err.println(staticNameToAssignments);
        }
    }   
    
    
    private JFieldDeclaration getFieldDecl(SIRGlobal s, String f) {
        JFieldDeclaration[] fields = s.getFields();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getVariable().getIdent().equals(f)) {
                return fields[i];
            }
        }
        throw new IllegalArgumentException("no JFieldDeclaration for " + f);
    }
    
    private List/*<JStatement>*/ getFieldAssignments(SIRGlobal s, final String f) {
        JMethodDeclaration[] methods = s.getMethods();
        assert methods.length == 1 : methods.length;
        // final here is bogus: List doesn't change but List
        // contents change.  final is necessary for reference from
        // inner class.
        final LinkedList fsAssignments = new LinkedList();
        final boolean[] isUsefulAssignment = {false};
        
        methods[0].accept(new KjcEmptyVisitor() {

            public void visitExpressionStatement(JExpressionStatement self,
                    JExpression expr) {
                isUsefulAssignment[0] = false;
                super.visitExpressionStatement(self,expr);
                if (isUsefulAssignment[0]) {
                    fsAssignments.addLast(self);
                }
            }
            public void visitAssignmentExpression(JAssignmentExpression self,
                    JExpression left, JExpression right) {
                super.visitAssignmentExpression(self, left, right);
                JExpression field = CommonUtils.lhsBaseExpr(left);
                if (field instanceof JFieldAccessExpression) {
                    JFieldAccessExpression fexpr = (JFieldAccessExpression) field;
                    // lhsBaseExpr should either have found an access to a field
                    // in 'this' static block -- static blocks should not assign
                    // to fields of other classes -- TODO: static check.
                    // (lhsBaseExpr could also have found an assigment to a local
                    // which could occur even if all fields are only given constant
                    // values since FieldProp does not remove dead code.
                    assert fexpr.getPrefix() instanceof JThisExpression : field;

                    if (fexpr.getIdent().equals(f)) {
                        isUsefulAssignment[0] = true;
                    }
                }
            }
        });
        
        return fsAssignments; //retval;
    }
    
    // --------------------------------------------------------------------
    // Propagate fields from static sections to their uses
    // --------------------------------------------------------------------
    
    /*
     * For each SIRStream element requiring static data give it its own copy.
     * 
     *  Fields are renamed apart in each copy.  We maintain a map from each
     *  renamed field name to the set of all renamings of the field.
     *
     *  Fields and code are inserted at the front of the SIRStram element and 
     *  the front of its init function respectively.  Changes for multiple
     *  fields will end up in arbitrary order with respect to each other.
     */
    private void propagateIntoStreams(SIRStream str) {
        IterFactory.createFactory().createIter(str).accept(
                new EmptyStreamVisitor() {
                    public void postVisitStream(SIRStream self,
                            SIRIterator iter) {
                        String streamIdent = self.getIdent();
                        Set/*<StaticAndField>*/ toPropagate = 
                            (Set)streamIdentToField.get(streamIdent);

                        Map /*<StaticAndField,String>*/ namesInStream = new HashMap();
                        if (toPropagate != null) { // if any references to statics in stream
                            for (Iterator it = toPropagate.iterator();
                                it.hasNext();) {
                                StaticAndField sf = (StaticAndField)it.next();

                                String newFieldName = newName(sf,staticNameToNewNames);
                                namesInStream.put(sf, newFieldName);
                                // We now have a static section and field name
                                // to propagate into the stream "self".
                                // (1) make a copy of the field decl and code
                                JFieldDeclaration newDecl = 
                                        (JFieldDeclaration)AutoCloner.deepCopy(
                                        (JFieldDeclaration)staticNameToFields.get(sf));
                                LinkedList/*<JStatement>*/ newAssignments = new LinkedList();
                                List/*<JStatement>*/ oldAssignments = 
                                    (List)staticNameToAssignments.get(sf);
                                for(Iterator i = oldAssignments.iterator(); i.hasNext();) {
                                    newAssignments.addLast(
                                            (JStatement)AutoCloner.deepCopy(
                                                    (JStatement)i.next()));
                                }
                                // (2) rename in the copy
                                mungFieldName(sf, newFieldName,newDecl,newAssignments);
 
                                // (3) propagate field decl
                                // (4) propagate code.
                                
                                // For a filter, which can have multiple functions,
                                // propgate as a field.
                                // For a container, which has only an init function,
                                // propagate as local variables in the init function.
                                // (If propagated as field, would not be correctly
                                // propagated through parameters by other propagation phases
                                propagateFieldDecl(self,newDecl);
                                propagateCode(self,newAssignments);
                            }
                            // (5) update all references in stream to the old names
                            updateReferences(self,namesInStream);
                        }
                    }
                });
        // (6) map from each synonym for a field name to the
        // set of all synonyms
        makeNameToName(staticNameToNewNames, nameToName);

    }
    
    /*
     * Change mapping from StaticAndField -> synonyms to
     * String -> synonyms
     */
    private static void makeNameToName(Map staticNameToNewNames, Map nameToName) {
        for (Iterator iter = staticNameToNewNames.entrySet().iterator(); iter.hasNext();) {
            Set synonyms = (Set)((Map.Entry)iter.next()).getValue();
            for (Iterator it = synonyms.iterator(); it.hasNext();) {
                nameToName.put(it.next(), synonyms);
            }
        }
    }
    

    /** How many variables have been renamed.  Used to uniquify names. */
    private static int counter = 0;
    
    /** Unique name -- we hope.
     * 
     *  Why is there no centralized facility for creating unique names?
     * 
     * @param oldName  a name in the program
     * @return         a (hopefully) unique name
     */
    private static String makeNewName(String oldName)
    {
        String name =  oldName + "__" + counter;
        counter++;
        return name;
    }
    
    private static String newName(StaticAndField sf, Map staticNameToNewNames) {
        String newname =  sf.getTheStatic() + "_" + makeNewName(sf.getTheField());
        addToMapValueSet(staticNameToNewNames, sf, newname);
        return newname;
    }
    
    /*
     * mung names.  Works in place!  pass it clones of theDecl, theCode.
     */
    private void mungFieldName(StaticAndField sf, final String newFieldName,
            JFieldDeclaration theDecl, List/*<JStatement>*/ theCode) {
        final String staticClassName = sf.getTheStatic();
        final String oldFieldName = sf.getTheField();
        

        for (Iterator it = theCode.iterator(); it.hasNext();) {
            ((JStatement)it.next()).accept(new KjcEmptyVisitor() {
                public void visitFieldExpression(JFieldAccessExpression self,
                        JExpression left, String ident) {
                    super.visitFieldExpression(self,left,ident);
                    JExpression newLeft = new JTypeNameExpression(left.getTokenReference(), staticClassName);
                    if (ident.equals(oldFieldName)) {
//                        self.setIdent(newFieldName);
//                        if (debugPrint) {System.err.println("Munged to " + newFieldName + " in " + self);}
                        self.setPrefix(newLeft);     //set prefix as would appear in client code
                    }
                    else if (debugPrint) {
                        System.err.println("Leaving untouched: "+ self);
                    }
                }
            });
        }
        
        theDecl.accept(new KjcEmptyVisitor() {
            public void visitFieldDeclaration(JFieldDeclaration self, 
                    int modifiers, CType type, String ident, JExpression expr) {
                    if (ident.equals(oldFieldName)) {
                        JVariableDefinition oldVar = self.getVariable();
                        JVariableDefinition newVar = new JVariableDefinition(
                            oldVar.getTokenReference(), oldVar.getModifiers(),
                            oldVar.getType(), newFieldName, oldVar.getValue());
                          
                        self.setVariable(newVar);
                        identToVarDefn.put(newFieldName,newVar);
                    }
            }
        });
    }
    
    /*
     * Put a field declaration into a SIRStream
     * either as a field (for fliters)
     * or in the beginning of an init as a local declaration (for containers)
     * If a local definition, save the mapping from name to JVariableDefinition
     * so that a unique copy of the definition will be used when replacing
     * fields.
     */
    private static void propagateFieldDecl(SIRStream self, JFieldDeclaration theDecl) {
        if (self instanceof SIRPhasedFilter) {
            self.addField(0,theDecl);
        } else {
            JVariableDefinition defn = theDecl.getVariable();
            self.getInit().getBody().addStatementFirst(
                    new JVariableDeclarationStatement(defn));
        }
    }
    
    /*
     * Put a list of statements into a SIRStream after any JVariableDeclarationStatement's
     */
    private static void propagateCode(SIRStream str, List/*<JStatement>*/ theCode) {
        // Add declaration or local and assignments in body of init.
        JMethodDeclaration init = str.getInit();
        JBlock body = init.getBody();
        ListIterator stmtIter = body.getStatementIterator();
        while (stmtIter.hasNext() && stmtIter.next() instanceof JVariableDeclarationStatement){}
        body.addAllStatements(stmtIter.previousIndex(), theCode);
    }

    /*
     * Update all field references of the form "StaticClass.oldName" to "this.newName"
     */
    private void updateReferences(final SIRStream str,
            final Map/* <StaticAndField,String> */namesInStream) {
        ReplacingVisitor visitor = new SLIRReplacingVisitor() {
            // need to change JFieldAccessExpression
            public Object visitFieldExpression(JFieldAccessExpression self,
                    JExpression left, String ident) {
                if (left instanceof JTypeNameExpression) {
                    String className = ((JTypeNameExpression) left).getClassType().getIdent();
                    String newFieldName = (String) namesInStream.get(new StaticAndField(className, ident));
                    if (newFieldName != null) {
                        if (str instanceof SIRPhasedFilter) {
                            // In a filter, update to new FieldAccessExpression
                            // this.newName  there is a redundant name in
                            // self.getField, but this is unused and not updated.
                            return new JFieldAccessExpression(self
                                    .getTokenReference(), new JThisExpression(
                                    left.getTokenReference()), newFieldName,
                                    self.getField());
                        } else {
                            // In a container, update to a JLocalVariableExpression
                            // only messy part is that declaration and all references
                            // must use a single JVariableDefinition object.  So keep
                            // track of in identToVarDefn.
                            return new JLocalVariableExpression(self
                                    .getTokenReference(),
                                    (JVariableDefinition) identToVarDefn
                                            .get(newFieldName));
                        }
                    } else {
                        if (debugPrint) {
                            System.err
                                    .println("Do not replace class and field "
                                            + className + "." + ident);
                        }
                        return super.visitFieldExpression(self, left, ident);
                    }
                } else {
                    if (debugPrint) {
                        System.err.println("Non-class-name " + left + " ."
                                + ident);
                    }
                    return super.visitFieldExpression(self, left, ident);
                }
            }
        };
        JMethodDeclaration[] methods = str.getMethods();
        for (int i = 0; i < methods.length; i++) {
            methods[i].setBody((JBlock) methods[i].getBody().accept(visitor));
        }

        // IterOverAllFieldsAndMethods.iterOverFieldsAndMethods(self, false,
        // true, visitor);
    }
    
    // -------------------------------------------------------------------
    // Utility routines that could well go elsewhere
    // -------------------------------------------------------------------

     
private static class IterOverAllFieldsAndMethods {
    /**
     * Operation to perform before visiting a SIRstream.
     * (for overriding)
     * 
     * @param   str    a SIRStream
     * @return  true if fields and methods in this SIRStream should be visited
     *          false otherwise.
     */
    
    protected boolean preVisit(SIRStream str) {
        return true;
    }
    
    /**
     * Operation to perform after visiting a SIRStream
     * 
     * @param str     a SIRStream
     */
    
    protected void postVisit(SIRStream str) {
    }
    
    /**
     * Visit all field and method definitions reachable from a SIRStream
     * 
     * Given a SIRStream (filter, pipeline, splitjoin, ...) for all
     * Iterate over the stream, and for each element use the passed
     * KjcVisitor to iterate over all field and method declarations.
     *
     * @param str        a SIRStream object -- root of traversal
     * @param            doFields == true to visit field declarations
     * @param            doMethods == true to vieis method declarations
     * @param kjcVisitor a visitor that will be used on all field and
     *                   method declarations.
    */
    public void iterOverAllFieldsAndMethods (SIRStream str, 
            boolean doFields, boolean doMethods,
            final KjcVisitor visitor) {
        final boolean _doFields = doFields;
        final boolean _doMethods = doMethods;
        IterFactory.createFactory().createIter(str).
            accept(new EmptyStreamVisitor() {

            public void preVisitStream(SIRStream self,
                        SIRIterator iter) {
                if (preVisit(self)) {
                    iterOverFieldsAndMethods(self,_doFields,_doMethods,visitor);
                    postVisit(self);
                }
            }
            });
    }


    /**
     * Visit all field and method declarations in a SIRStream element.
     * 
     * Given a SIRStream element, find all field declarations and methods
     * defined in the stream element, and have them accept the passed
     * visitor.
     *
     * @param str        a SIRStream object (filter, splitjoin, ...)
     * @param            doFields == true to visit field declarations
     * @param            doMethods == true to vieis method declarations
     * @param kjcVisitor a visitor that will be used on all field and
     *                   method declarations. 
     *
     */
    public static void iterOverFieldsAndMethods(SIRStream str,
            boolean doFields, boolean doMethods, KjcVisitor visitor) {
        if (doFields) {
            JFieldDeclaration[] fields = str.getFields();
            for (int i = 0; i < fields.length; i++) {
                fields[i].accept(visitor);
            }
        }
        if (doMethods) {
            JMethodDeclaration[] methods = str.getMethods();
            for (int i = 0; i < methods.length; i++) {
                methods[i].accept(visitor);
            }
        }
    }
}    

/**
     * Given a map from a key to a set of values, add another value for a key. 
     * 
     * This version does not alow you to select the Set implmentation to use.
     * 
     */
    
    private static void addToMapValueSet(Map/*<K,Set<V>>*/ map, 
                                  Object/*<K>*/ key,
                                  Object/*<V>*/ val) {
        Set/*<V>*/ oldVal = (Set)map.get(key);
        if (oldVal == null) {
            HashSet/*<V>*/ values = new HashSet();
            values.add(val);
            map.put(key,values);
        } else { 
            oldVal.add(val);
        }
    }



}
