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
import at.dms.compiler.TokenReference;

/**
 * StaticsProp.propagate propagates constants from 'static' sections.
 * 
 * <p>StaticsProp.propagate does all the work.</p>
 * 
 * <p>This pass is expected to be run after lowering.RenameAll but
 * before any other lowering pass.</p>
 * 
 * <p>This pass is currently written under the simplfying assumption that
 * every static section can be unwound to be assignments of constants to 
 * fields -- so no field depends on the value of any other field.  If this
 * assumption does not hold, then we need to create a joint back-slice of 
 * the static code for each filter that we are propagating into.</p>
 * 
 * <p>We also make the (semantically correct, but possibly inefficient) 
 * decision to propagate the static information to each filter or combiner
 * that uses it.  If the static fields can not be removed by constant
 * propagation, we should instead probably make one copy per physical
 * location in the final program.  This is difficult because we want
 * StaticsProp run early in the backends, while physical locations are
 * often not assigned until very late in the backends,</p>
 *
 * @author Allyn Dimock
 *
 */
public class StaticsProp {
    
    static boolean debugPrint = false;
    
    // -------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------
    
    private StaticsProp(Set<SIRGlobal> theStatics) {
        
        // during processing we will want 
        //   the static sections.
        //   their names,
        //   and a map from names to sections.
        
        HashSet<String> staticsNames = new HashSet<String>();
        HashMap<String, SIRGlobal> namesToStatics = new HashMap<String, SIRGlobal>();
        
        for (Iterator<SIRGlobal> i = theStatics.iterator(); i.hasNext(); ) {
            SIRGlobal aStatic = i.next();
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
    
    // Names of all static sections.
    private Set<String> staticsNames;

    // map from name of static section to the section
    private Map<String, SIRGlobal> namesToStatics;

    // map from the name of a SIRStream element to all
    // static fields that it uses (by static section name, field name)
    private Map<String,Set<StaticAndField>> streamIdentToField =
	new HashMap<String,Set<StaticAndField>>();

    // the current SIRStream element name for iterators. (Allows me to 
    // use the value in code in inner classes without having to define them
    // inside the method that uses them...
    private String currentStreamIdent;
    
    // map from a static section name and field name to the code for
    // declaring (and statically initializing) the field.
    private Map<StaticAndField,JFieldDeclaration> staticNameToFields =
	new HashMap<StaticAndField,JFieldDeclaration>();
 
    // map from a static section name and a field name to the code that
    // is needed to assign value(s) to the field.
    private Map<StaticAndField,List<JStatement>> staticNameToAssignments =
	new HashMap<StaticAndField,List<JStatement>>();

    // map from a static section and field name to the set of synonyms for
    // the field.
    private Map<StaticAndField,Set<String>> staticNameToNewNames = 
	new HashMap<StaticAndField,Set<String>>();

    // map from each propagated field name to a set
    // of synonyms (including the name itself)
    private Map<String,Set<String>> nameToName = new HashMap<String,Set<String>>();

    
    // for conversion to local variables, there must be a single definition of the variable.
    private final Map<String,JVariableDefinition> identToVarDefn =
	new HashMap<String,JVariableDefinition>();

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
            if (o != null && o.getClass().equals(this.getClass())) {
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
     * The top-level for propagating data from static sections into the stream
     * graph.
     * 
     * <p>Of course, for some static data -- using a static section to set
     * parameters, or push, peek, or pop rates -- it is necessary that the
     * static data be constants that can be propagated to place with FieldProp
     * in the streams.</p>
     * 
     * <p>Warning: if fusing filters with control constructs, please note that
     * information from static sections appears in <b>filters</b> as <b>fields</b>
     * so that multiple phases (even just init and work) have access to the
     * values. In <b>containers</b> the information appears as
     * <b>local variables</b>. The reason for the difference is that field 
     * propagation does not propagate information far enough for containers, so
     * we need to use constant prop based on constant values of local variables.
     * This presents a non-uniform interface for using the returned map.</p>
     * 
     * 
     * 
     * @param str
     *            The stream program
     * @param theStatics
     *            a set of static sections that need to be propagated through
     *            the stream.
     * 
     * @return a map of associations between propagated data. This information
     *         may be useful in a fusion: if two filters with associated data
     *         are fused, only one copy is needed.  The map is from a field
     *         or variable name as a string to a set of strings that are 
     *         synonyms for the variable name.
     * 
     */
    public static Map propagate(SIRStream str, Set<SIRGlobal> theStatics) {
        linearizeStatics(theStatics);

        if (debugPrint) {
            System.err.println("// StaticsProp: globals after linearization");
            for (Iterator<SIRGlobal> it = theStatics.iterator(); it.hasNext();) {
                SIRToStreamIt.runOnGlobal(it.next());
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
    private static void linearizeStatics(Set<SIRGlobal> theStatics) {
        // Temporarily turn off structure elimination since
        // it should not have been run on the stream yet if passes are ordered 
	// correctly and should not be run on the static sections.
        boolean oldStructs = KjcOptions.struct;
        KjcOptions.struct = false;
        for (Iterator<SIRGlobal> i = theStatics.iterator(); i.hasNext();) {
            final SIRGlobal s = i.next();

            //prepass: any fields in a static section are static.
            // In the case of setting up array sizes, this becomes
            // important: a reference to "this" in setting up a 
            // static field should become an explicit reference to the
            // static section. (Not just good Java coding practice but
            // also necessary for propagation so that "this" does not
            // get captured at target site when propagated.)
            // Take care of this for fields, ConstantProp will take care
            // of for methods.  eg:
            // int N;
            // int[N] myArray;
            // init {N = 10;}
            // should result in an array of size 10, not an array of
            // unknown length.

            JFieldDeclaration[] fs = s.getFields();
            for (int j = 0; j < fs.length; j++) {
                JFieldDeclaration f = fs[j];
                fs[j] = (JFieldDeclaration)f.accept(new SLIRReplacingVisitor() {
                    public Object visitFieldExpression(JFieldAccessExpression self,
                            JExpression left, String fieldIdent) {
                        JFieldAccessExpression f1 = 
                            (JFieldAccessExpression)super.visitFieldExpression(self,left,fieldIdent);
                        if (left instanceof JThisExpression) {
                            f1.setPrefix(new JTypeNameExpression(
                                    left.getTokenReference(),
                                    s.getIdent()));
                        }
                        return f1;
                    }
                    public Object visitNameExpression(JNameExpression self, JExpression left, String ident) {
                        JNameExpression f1 = (JNameExpression)super.visitNameExpression(self,left, ident);
                        if (left == null || left instanceof JThisExpression) {
                            JFieldAccessExpression f2 = 
                                new JFieldAccessExpression(self.getTokenReference(),
                                        new JTypeNameExpression(self.getTokenReference(),
                                                s.getIdent()),
                                        ident);
                            return f2;
                        }
                        return f1;
                    }
                });
            }
            s.setFields(fs);
            
            
            // Hopefully, ConstantProp will evaluate any code
            // in the init portion of the static block and update
            // fields to be constant.
            
            ConstantProp.propagateAndUnroll(s, false);

            // In this version there is no guarantee that any static
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
    
    // for all field names in static sections that are referenced from the 
    // StreamIt program, get the JFieldDeclaration in staticNameToFields and
    // get the assignments used in initializing the field in 
    // staticNameToAssignments.

    private void getCodeToPropagate() {
        Set<StaticAndField> usedStaticAndFields = new HashSet<StaticAndField>();
        // from map key->Set<StaticAndField>  to Set<StaticAndField>
        for (Iterator<Set<StaticAndField>> iter = streamIdentToField.values().iterator(); 
	     iter.hasNext();) {
            usedStaticAndFields.addAll(iter.next());
        }

        for (Iterator iter = usedStaticAndFields.iterator(); iter.hasNext();) {
            StaticAndField sNameAndField = (StaticAndField)iter.next();
            String thisStaticName = sNameAndField.getTheStatic();
            String thisFieldName = sNameAndField.getTheField();
            SIRGlobal thisStatic = namesToStatics.get(thisStaticName);
            
            staticNameToFields.put(sNameAndField,
                                   getFieldDecl(thisStatic, thisFieldName));
            staticNameToAssignments.put(sNameAndField,
                                        getFieldAssignments(thisStatic, 
							    thisFieldName));
        }
        if (debugPrint) {
            System.err.println(staticNameToFields);
            System.err.println(staticNameToAssignments);
        }
    }   
    
    // return the JFieldDeclaration in SIRGlobal s for field name f.
    private JFieldDeclaration getFieldDecl(SIRGlobal s, String f) {
        JFieldDeclaration[] fields = s.getFields();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getVariable().getIdent().equals(f)) {
                return fields[i];
            }
        }
        throw new IllegalArgumentException("no JFieldDeclaration for " + f);
    }

    // Return a list (in original order order) of assignments in static 
    // sections to a field with name f.
    private List<JStatement> getFieldAssignments(SIRGlobal s, 
						     final String f) {
        JMethodDeclaration[] methods = s.getMethods();
        assert methods.length == 1 : methods.length;
        // final here is bogus: List doesn't change but List
        // contents change.  final is necessary for reference from
        // inner class.
        final LinkedList<JStatement> fsAssignments = new LinkedList<JStatement>();
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
                                                  JExpression left, 
                                                  JExpression right) {
                super.visitAssignmentExpression(self, left, right);
                JExpression field = CommonUtils.lhsBaseExpr(left);
                if (field instanceof JFieldAccessExpression) {
                    JFieldAccessExpression fexpr = 
			(JFieldAccessExpression) field;
                    // lhsBaseExpr should either have found an access to a 
                    // field in 'this' static block -- static blocks should not
                    //  assign to fields of other classes -- TODO:static check.
                    // (lhsBaseExpr could also have found an assigment to a 
                    // local which could occur even if all fields are only 
                    // given constant values since FieldProp does not remove 
		    // dead code.
                    assert fexpr.getPrefix() instanceof JThisExpression :field;

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
     * For each filter or container requiring static data:give it its own copy.
     * 
     *  Fields are renamed apart in each copy.  We maintain a map from each
     *  renamed field name to the set of all renamings of the field.
     *
     *  Fields and code are inserted at the front of the filter and 
     *  the front of its init function respectively.
     *    
     *  Fields and code for a container are both inserted at the beginning 
     *  of its init function.
     *  
     *  Changes for multiple fields will end up in arbitrary order with 
     *  respect to each other.
     */
    private void propagateIntoStreams(SIRStream str) {
        IterFactory.createFactory().createIter(str).accept(
            new EmptyStreamVisitor() {
                public void postVisitStream(SIRStream self,
                        SIRIterator iter) {
                    String streamIdent = self.getIdent();
                    Set<StaticAndField> toPropagate = 
                        streamIdentToField.get(streamIdent);
                        
                    Map<StaticAndField,String> namesInStream = 
			new HashMap<StaticAndField,String>();
		    // if any references to statics in stream
                    if (toPropagate != null) { 
                        for (Iterator it = toPropagate.iterator();
                          it.hasNext();) {
                            StaticAndField sf = (StaticAndField)it.next();
                                
                            String newFieldName = 
				newName(sf,staticNameToNewNames);
                            namesInStream.put(sf, newFieldName);
                            // We now have a static section and field name
                            // to propagate into the stream "self".
                            // (1) make a copy of the field decl and code
                            JFieldDeclaration newDecl = 
			      (JFieldDeclaration) ObjectDeepCloner
			      .deepCopy(
				(JFieldDeclaration)staticNameToFields.get(sf));
                            LinkedList<JStatement> newAssignments = 
				new LinkedList<JStatement>();
                            List<JStatement> oldAssignments = 
                                staticNameToAssignments.get(sf);
                            for (Iterator i = oldAssignments.iterator(); i
                                    .hasNext();) {
                                newAssignments
                                        .addLast((JStatement) ObjectDeepCloner
                                                .deepCopy((JStatement) i
                                                        .next()));
                            }
                            // (2) rename in the copy
                            mungFieldName(sf, 
					  newFieldName,newDecl,newAssignments);
                                
                            // (3) propagate field decl
                            // (4) propagate code.
                                  
                            // For a filter, which can have multiple functions,
                            // propgate as a field.
                            // For a container, which has only an init function,
                            // propagate as local variables in the init function.
                            // (If propagated as field, would not be correctly
                            // propagated through parameters by other 
			    // propagation phases
                            propagateFieldDecl(self,newDecl);
                            propagateCode(self,newAssignments);
                        }
                        // (5) update all references to the old names in 
			// the stream
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
    private static void makeNameToName(Map<StaticAndField,Set<String>> staticNameToNewNames,
            Map<String,Set<String>> nameToName) {
        for (Iterator<Map.Entry<StaticAndField,Set<String>>> iter = 
                staticNameToNewNames.entrySet().iterator(); iter.hasNext();) {
            Set<String> synonyms = iter.next().getValue();
            for (Iterator<String> it = synonyms.iterator(); it.hasNext();) {
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
    
    // make a new name and add a mapping for the new name to 
    // staticNameToNewNames
    private static String newName(StaticAndField sf, 
				  Map<StaticAndField,Set<String>> staticNameToNewNames) {
        String newname =  sf.getTheStatic() + "_" 
	                  + makeNewName(sf.getTheField());
        addToMapValueSet(staticNameToNewNames, sf, newname);
        return newname;
    }
    

    // Mung names.  Works in place!  pass it clones of theDecl, theCode.
    // Uses visitors to JFieldAccessExpression, to JFieldDeclaration
    // for field names.
    private void mungFieldName(StaticAndField sf, final String newFieldName,
                               JFieldDeclaration theDecl, 
			       List<JStatement> theCode) {
        final String staticClassName = sf.getTheStatic();
        final String oldFieldName = sf.getTheField();
        

        for (Iterator it = theCode.iterator(); it.hasNext();) {
            ((JStatement)it.next()).accept(new KjcEmptyVisitor() {
                public void visitFieldExpression(JFieldAccessExpression self,
                                                 JExpression left, 
                                                 String ident) {
                    super.visitFieldExpression(self,left,ident);
                    JExpression newLeft = 
                      new JTypeNameExpression(left.getTokenReference(), 
                                              staticClassName);
                    if (ident.equals(oldFieldName)) {
                        //set prefix as would appear in client code
                        self.setPrefix(newLeft);
                    }
                    else if (debugPrint) {
                        System.err.println("Leaving untouched: "+ self);
                    }
                }
                });
        }
        
        theDecl.accept(new KjcEmptyVisitor() {
                public void visitFieldDeclaration(JFieldDeclaration self, 
                                                  int modifiers, 
                                                  CType type, 
                                                  String ident, 
                                                  JExpression expr) {
                    if (ident.equals(oldFieldName)) {
                        JVariableDefinition oldVar = self.getVariable();
                        JVariableDefinition newVar = new JVariableDefinition(
                                oldVar.getTokenReference(), 
                                oldVar.getModifiers(),
                                oldVar.getType(), 
                                newFieldName, 
                                oldVar.getValue());
                          
                        self.setVariable(newVar);
                        identToVarDefn.put(newFieldName,newVar);
                    }
                }
            });
    }
    
    /*
     * Put a field declaration into a SIRStream
     * either as a field (for filters)
     * or in the beginning of an init as a local declaration (for containers)
     * If a local definition, save the mapping from name to JVariableDefinition
     * so that a unique copy of the definition will be used when replacing
     * fields.
     */
    private static void propagateFieldDecl(SIRStream self, 
					   JFieldDeclaration theDecl) {
        if (self instanceof SIRPhasedFilter) {
            self.addField(0,theDecl);
        } else {
            JVariableDefinition defn = theDecl.getVariable();
            self.getInit().getBody().addStatementFirst(
                    new JVariableDeclarationStatement(defn));
        }
    }
    
    /*
     * Put a list of statements into the init function of a SIRStream 
     * after any JVariableDeclarationStatement's
     */
    private static void propagateCode(SIRStream str, 
				      List/*<JStatement>*/ theCode) {
        // Add declaration or local and assignments in body of init.
        JMethodDeclaration init = str.getInit();
        JBlock body = init.getBody();
        ListIterator stmtIter = body.getStatementIterator();
        // iterate past JVariableDeclarationStatement's
        while (stmtIter.hasNext() 
               && stmtIter.next() instanceof JVariableDeclarationStatement){}
        // add
        body.addAllStatements(stmtIter.nextIndex(), theCode);
    }

    /*
     * Update all field references of the form "StaticClass.oldName" 
     * to "this.newName"
     */
    private void updateReferences(final SIRStream str,
                       final Map<StaticAndField,String>namesInStream) {
        ReplacingVisitor visitor = new SLIRReplacingVisitor() {
                // need to change JFieldAccessExpression
                public Object visitFieldExpression(JFieldAccessExpression self,
                                                   JExpression left, 
						   String ident) {
                    if (left instanceof JTypeNameExpression) {
                        String className = ((JTypeNameExpression) left).
			    getClassType().getIdent();
                        String newFieldName = namesInStream.get(
			     new StaticAndField(className, ident));
                        if (newFieldName != null) {
                            if (str instanceof SIRPhasedFilter) {
                                // In a filter, update to new 
				// FieldAccessExpression this.newName  
				// there is a redundant name in self.getField,
				// but this is unused and not updated.
                                return new JFieldAccessExpression(self
                                    .getTokenReference(), 
                                    new JThisExpression(left.getTokenReference()), 
                                    newFieldName,
                                    self.getField());
                            } else {
                                // In a container, update to a 
				// JLocalVariableExpression.  The only messy 
				// part is that declaration and all references
                                // must use a single JVariableDefinition 
				// object.  So keep track of in identToVarDefn.
                                return new JLocalVariableExpression(self
                                     .getTokenReference(),
                                    identToVarDefn.get(newFieldName));
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
        // send through methods
        JMethodDeclaration[] methods = str.getMethods();
        for (int i = 0; i < methods.length; i++) {
            methods[i].accept(visitor);
        }
        // send through fields, so that it hits the static array
        // dimensions as part of field declarations
        JFieldDeclaration[] fields = str.getFields();
        for (int i = 0; i < fields.length; i++) {
            fields[i].accept(visitor);
        }
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
         * @return  true if fields and methods in this SIRStream should be 
	 *               visited.
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
         * Visit all field and method definitions reachable from a SIRStream.
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
                                                 boolean doFields, 
						 boolean doMethods,
                                                 final KjcVisitor visitor) {
            final boolean _doFields = doFields;
            final boolean _doMethods = doMethods;
            IterFactory.createFactory().createIter(str).
                accept(new EmptyStreamVisitor() {

                        public void preVisitStream(SIRStream self,
                                                   SIRIterator iter) {
                            if (preVisit(self)) {
                                iterOverFieldsAndMethods(self,_doFields,
							 _doMethods,visitor);
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
                                                    boolean doFields, 
						    boolean doMethods, 
						    KjcVisitor visitor) {
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
    
    private static <K,V> void addToMapValueSet(Map<K,Set<V>> map, K key, V val) {
        Set<V> oldVal = map.get(key);
        if (oldVal == null) {
            HashSet<V> values = new HashSet<V>();
            values.add(val);
            map.put(key,values);
        } else { 
            oldVal.add(val);
        }
    }



}
