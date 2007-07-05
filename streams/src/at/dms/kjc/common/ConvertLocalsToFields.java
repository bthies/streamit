package at.dms.kjc.common;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import java.util.*;

/**
 * This class converts certain (large) local variables into fields so
 * that they are eventually declared as globals rather than locals,
 * thereby avoiding stack-overflow problems.
 *
 * THIS TRANSFORMATION IS NOT SAFE YET!-- because it does not
 * re-initialize the field in place of its previous declaration.
 * See the TODO list below for how to make this safe.
 *
 * IF YOU FIX IT TO BE SAFE, please remove safety warning in strc.
 */
public class ConvertLocalsToFields {

    /**
     * Converts locals to fields for all methods of all filters in
     * 'str'.
     */
    public static void doit(SIRStream str) {
        IterFactory.createFactory().createIter(str).accept(new EmptyStreamVisitor() {
                public void visitFilter(SIRFilter self,
                                        SIRFilterIter iter) {
                    convertFilter(self);
                }
            });
    }

    /**
     * Converts eligible locals in all methods of given filter.
     */
    private static void convertFilter(SIRFilter filter) {
        // maps from old local variables to the name of the associated
        // field
        final Set<JVariableDefinition> locals = new HashSet<JVariableDefinition>();
        // counter to make new field names unique
        final int numProcessed[] = {0};

        // mutate code in the methods
        for (int i=0; i<filter.getMethods().length; i++) {
            filter.getMethods()[i].accept(new SLIRReplacingVisitor() {

                    // variable definitions get replaced by initializations
                    public Object visitVariableDefinition(JVariableDefinition self,
                                                          int modifiers,
                                                          CType type,
                                                          String ident,
                                                          JExpression expr) {
                        if (shouldConvert(type)) {
                            // remember and rename
                            locals.add(self);
                            self.setIdent(ident + "__" + numProcessed[0]++);
                            // TODO: 
                            // - change to initializer (array initializer or memset to 0)
                            // - perhaps avoid calling memset for small arrays
                            // - might need to raise variable decls later
                            // - might need to lift new statements out
                            //   of jvariabledeclaration statements that
                            //   contain mulitple vardefs
                            return new JEmptyStatement();
                        } else {
                            return self;
                        }
                    }

                    // variable references get replaced by field references
                    public Object visitLocalVariableExpression(JLocalVariableExpression self,
                                                               String ident) {
                        if (locals.contains(self.getVariable())) {
                            JFieldAccessExpression field = new JFieldAccessExpression(self.getIdent());
                            field.setType(self.getType()); // for good measure
                            return field;
                        } else {
                            return self;
                        }
                    }
                });
        }

        // add the fields to the filter
        List fields = new LinkedList(Arrays.asList(filter.getFields()));
        for (Iterator<JVariableDefinition> it = locals.iterator(); it.hasNext(); ) {
            JVariableDefinition local = it.next();
            fields.add(new JFieldDeclaration(local));
        }
        filter.setFields((JFieldDeclaration[])fields.toArray(new JFieldDeclaration[0]));
    }

    /**
     * Determines whether or not we should convert a given variable
     * type.
     */
    private static boolean shouldConvert(CType type) {
        // for now, convert all structs and arrays.  Once we can get
        // sizes of structs in compiler, only convert big structs.
        return type.isArrayType() || type.isClassType();
    }
    
}
