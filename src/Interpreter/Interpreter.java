package Interpreter;

import AST.*;

import java.util.*;

public class Interpreter {

    private final TranNode top;

    /** Constructor - get the interpreter ready to run. Set members from parameters and "prepare" the class.
     *
     * Store the tran node.
     * Add any built-in methods to the AST
     * @param top - the head of the AST
     */
    public Interpreter(TranNode top) {
        // console.write
        ClassNode console = new ClassNode();
        console.name = "console";
        ConsoleWrite write = new ConsoleWrite();
        write.name = "write";
        write.isPrivate = false;
        write.isShared = true;
        console.methods.add(write);
        top.Classes.add(console);
        // number.times()
        ClassNode number = new ClassNode();
        number.name = "number";

        this.top = top;
    }

    /**
     * This is the public interface to the interpreter. After parsing, we will create an interpreter and call start to
     * start interpreting the code.
     *
     * Search the classes in Tran for a method that is "isShared", named "start", that is not private and has no parameters
     * Call "InterpretMethodCall" on that method, then return.
     * Throw an exception if no such method exists.
     */
    public void start() throws RuntimeException {
        for (ClassNode c : top.Classes) {
            for (MethodDeclarationNode m : c.methods) {
                if (m.name.equals("start") && m.isShared && !m.isPrivate && m.parameters.isEmpty()) {
                    interpretMethodCall(Optional.empty(), m, new LinkedList<InterpreterDataType>());
                    return;
                }
            }
        }
        throw new RuntimeException("No entrypoint found. Is 'shared start()' defined?");
    }

    //              Running Methods

    /**
     * Find the method (local to this class, shared (like Java's system.out.print), or a method on another class)
     * Evaluate the parameters to have a list of values
     * Use interpretMethodCall() to actually run the method.
     *
     * Call GetParameters() to get the parameter value list
     * Find the method. This is tricky - there are several cases:
     * someLocalMethod() - has NO object name. Look in "object"
     * console.write() - the objectName is a CLASS and the method is shared
     * bestStudent.getGPA() - the objectName is a local or a member
     *
     * Once you find the method, call InterpretMethodCall() on it. Return the list that it returns.
     * Throw an exception if we can't find a match.
     * @param object - the object we are inside right now (might be empty)
     * @param locals - the current local variables
     * @param mc - the method call
     * @return - the return values
     */
    private List<InterpreterDataType> findMethodForMethodCallAndRunIt(Optional<ObjectIDT> object, HashMap<String, InterpreterDataType> locals, MethodCallStatementNode mc) throws RuntimeException {
        List<InterpreterDataType> values = getParameters(object, locals, mc);
        // local method
        if (mc.objectName.isEmpty() && object.isPresent()) {
            for (MethodDeclarationNode m : object.get().astNode.methods)
                if (m.name.equals(mc.methodName))
                    return interpretMethodCall(object, m, values);
            throw new RuntimeException(String.format("Local method '%s' not found", mc.methodName));
        }
//        MethodDeclarationNode toRun = null;
        if (mc.objectName.isEmpty())
            throw new RuntimeException("Expected object identifier in method call");
        // local variable
        InterpreterDataType obj = locals.get(mc.objectName.get());
        if (obj instanceof ReferenceIDT ref)
            if (ref.refersTo.isPresent())
                for (MethodDeclarationNode m : ref.refersTo.get().astNode.methods)
                    if (m.name.equals(mc.methodName))
                        return interpretMethodCall(ref.refersTo, m, values);
        // member variable
        if (object.isPresent()) {
            InterpreterDataType memberMethod = object.get().members.get(mc.objectName.get());
            if (memberMethod instanceof ReferenceIDT mm)
                if (mm.refersTo.isPresent())
                    for (MethodDeclarationNode m : mm.refersTo.get().astNode.methods)
                        if (m.name.equals(mc.methodName))
                            return interpretMethodCall(object, m, values);
        }
        // static method call
        for (ClassNode c : top.Classes)
            if (c.name.equals(mc.objectName.get()))
                for (MethodDeclarationNode m : c.methods)
                    if (m.name.equals(mc.methodName) && m.isShared)
                        return interpretMethodCall(object, m, values);
        throw new RuntimeException(String.format("Unable to find method '%s', was it declared?", mc.methodName));
    }

    /**
     * Run a "prepared" method (found, parameters evaluated)
     * This is split from findMethodForMethodCallAndRunIt() because there are a few cases where we don't need to do the finding:
     * in start() and dealing with loops with iterator objects, for example.
     *
     * Check to see if "m" is a built-in. If so, call Execute() on it and return
     * Make local variables, per "m"
     * If the number of passed in values doesn't match m's "expectations", throw
     * Add the parameters by name to locals.
     * Call InterpretStatementBlock
     * Build the return list - find the names from "m", then get the values for those names and add them to the list.
     * @param object - The object this method is being called on (might be empty for shared)
     * @param m - Which method is being called
     * @param values - The values to be passed in
     * @return the returned values from the method
     */
    private List<InterpreterDataType> interpretMethodCall(Optional<ObjectIDT> object, MethodDeclarationNode m, List<InterpreterDataType> values) {
        if (m instanceof BuiltInMethodDeclarationNode md)
            return md.Execute(values);
        if (values.size() != m.parameters.size())
            throw new RuntimeException(String.format("Method '%s' expected %d parameters, got %d", m.name, m.parameters.size(), values.size()));
        HashMap<String, InterpreterDataType> locals = new HashMap<>();
        for (int i = 0; i < values.size(); i++)
            locals.put(m.parameters.get(i).name, values.get(i));
        for (VariableDeclarationNode vd : m.returns) {
            if (locals.containsKey(vd.name))
                throw new RuntimeException(String.format("Variable '%s' is already defined", vd.name));
            locals.put(vd.name, instantiate(vd.type));
        }
        for (VariableDeclarationNode vd : m.locals) {
            if (locals.containsKey(vd.name))
                throw new RuntimeException(String.format("Variable '%s' is already defined", vd.name));
            locals.put(vd.name, instantiate(vd.type));
        }

        interpretStatementBlock(object, m.statements, locals);
        LinkedList<InterpreterDataType> result = new LinkedList<>();
        for (VariableDeclarationNode v : m.returns)
            result.add(locals.get(v.name));
        return result;
    }

    //              Running Constructors

    /**
     * This is a special case of the code for methods. Just different enough to make it worthwhile to split it out.
     *
     * Call GetParameters() to populate a list of IDT's
     * Call GetClassByName() to find the class for the constructor
     * If we didn't find the class, throw an exception
     * Find a constructor that is a good match - use DoesConstructorMatch()
     * Call InterpretConstructorCall() on the good match
     * @param callerObj - the object that we are inside when we called the constructor
     * @param locals - the current local variables (used to fill parameters)
     * @param mc  - the method call for this construction
     * @param newOne - the object that we just created that we are calling the constructor for
     */
    private void findConstructorAndRunIt(Optional<ObjectIDT> callerObj, HashMap<String, InterpreterDataType> locals, MethodCallStatementNode mc, ObjectIDT newOne) {
        List<InterpreterDataType> params = getParameters(callerObj, locals, mc);
        Optional<ClassNode> cls = getClassByName(mc.methodName);
        if (cls.isEmpty())
            throw new RuntimeException(String.format("Unable to find class '%s'", newOne.astNode.name));
        ConstructorNode construct = null;
        for (ConstructorNode c : cls.get().constructors)
            if (doesConstructorMatch(c, mc, params)) {
                construct = c;
                break;
            }
        if (construct == null)
            throw new RuntimeException(String.format("Unable to find suitable constructor for class '%s'", newOne.astNode.name));
        interpretConstructorCall(newOne, construct, params);
    }

    /**
     * Similar to interpretMethodCall, but "just different enough" - for example, constructors don't return anything.
     *
     * Creates local variables (as defined by the ConstructorNode), calls Instantiate() to do the creation
     * Checks to ensure that the right number of parameters were passed in, if not throw.
     * Adds the parameters (with the names from the ConstructorNode) to the locals.
     * Calls InterpretStatementBlock
     * @param object - the object that we allocated
     * @param c - which constructor is being called
     * @param values - the parameter values being passed to the constructor
     */
    private void interpretConstructorCall(ObjectIDT object, ConstructorNode c, List<InterpreterDataType> values) {
        if (values.size() != c.parameters.size())
            throw new RuntimeException(String.format("Constructor expected %d parameters, got %d", c.parameters.size(), values.size()));
        HashMap<String, InterpreterDataType> locals = new HashMap<>();
        for (int i = 0; i < values.size(); i++)
            locals.put(c.parameters.get(i).name, values.get(i));
        for (VariableDeclarationNode vd : c.locals) {
            if (locals.containsKey(vd.name))
                throw new RuntimeException(String.format("Variable '%s' is already defined", vd.name));
            locals.put(vd.name, instantiate(vd.type));
        }
        interpretStatementBlock(Optional.of(object), c.statements, locals);
    }

    //              Running Instructions

    /**
     * Given a block (which could be from a method or an "if" or "loop" block, run each statement.
     * Blocks, by definition, do ever statement, so iterating over the statements makes sense.
     *
     * For each statement in statements:
     * check the type:
     *      For AssignmentNode, FindVariable() to get the target. Evaluate() the expression. Call Assign() on the target with the result of Evaluate()
     *      For MethodCallStatementNode, call doMethodCall(). Loop over the returned values and copy the into our local variables
     *      For LoopNode - there are 2 kinds.
     *          Setup:
     *          If this is a Loop over an iterator (an Object node whose class has "iterator" as an interface)
     *              Find the "getNext()" method; throw an exception if there isn't one
     *          Loop:
     *          While we are not done:
     *              if this is a boolean loop, Evaluate() to get true or false.
     *              if this is an iterator, call "getNext()" - it has 2 return values. The first is a boolean (was there another?), the second is a value
     *              If the loop has an assignment variable, populate it: for boolean loops, the true/false. For iterators, the "second value"
     *              If our answer from above is "true", InterpretStatementBlock() on the body of the loop.
     *       For If - Evaluate() the condition. If true, InterpretStatementBlock() on the if's statements. If not AND there is an else, InterpretStatementBlock on the else body.
     * @param object - the object that this statement block belongs to (used to get member variables and any members without an object)
     * @param statements - the statements to run
     * @param locals - the local variables
     */
    private void interpretStatementBlock(Optional<ObjectIDT> object, List<StatementNode> statements, HashMap<String, InterpreterDataType> locals) {
        for (StatementNode statement : statements) {
            switch (statement) {
                case AssignmentNode a -> {
                    InterpreterDataType target = findVariable(a.target.name, locals, object);
                    InterpreterDataType value = evaluate(locals, object, a.expression);
                    target.Assign(value);
                }
                case MethodCallStatementNode mc -> findMethodForMethodCallAndRunIt(object, locals, mc);
                case IfNode i -> {
                    BooleanIDT condition = (BooleanIDT) evaluate(locals, object, i.condition);
                    if (condition.Value)
                        interpretStatementBlock(object, i.statements, locals);
                    else if (i.elseStatement.isPresent())
                        interpretStatementBlock(object, i.elseStatement.get().statements, locals);
                }
                case LoopNode loop -> {
                    if (loop.expression instanceof ReferenceIDT ref
                            && ref.refersTo.isPresent()
                            && ref.refersTo.get().astNode.interfaces.contains("iterator")
                    ) {
                        ObjectIDT iterable = ref.refersTo.get();
                        MethodDeclarationNode getNext = null;
                        for (MethodDeclarationNode m : iterable.astNode.methods)
                            if (m.name.equals("getNext")) {
                                getNext = m;
                                break;
                            }
                        List<InterpreterDataType> next = interpretMethodCall(object, getNext, new LinkedList<InterpreterDataType>());
                        boolean hasNext = ((BooleanIDT) next.get(0)).Value;
                        InterpreterDataType nextItem = next.get(1);
                        if (loop.assignment.isPresent())
                            findVariable(loop.assignment.get().name, locals, object).Assign(nextItem);
                        while (hasNext) {
                            interpretStatementBlock(object, loop.statements, locals);
                            next = interpretMethodCall(object, getNext, new LinkedList<InterpreterDataType>());
                            hasNext = ((BooleanIDT) next.get(0)).Value;
                            nextItem = next.get(1);
                            if (loop.assignment.isPresent())
                                findVariable(loop.assignment.get().name, locals, object).Assign(nextItem);
                        }
                    } else if (loop.expression instanceof MethodCallExpressionNode mc
                            && mc.objectName.isPresent()
                            && findVariable(mc.objectName.get(), locals, object) instanceof NumberIDT num
                            && mc.methodName.equals("times")
                    ) {
                        for (int i = 0; i < num.Value; i++) {
                            interpretStatementBlock(object, loop.statements, locals);
                            if (loop.assignment.isPresent())
                                findVariable(loop.assignment.get().name, locals, object).Assign(new NumberIDT(i + 1));
                        }
                    } else {
                        BooleanIDT exp = (BooleanIDT) evaluate(locals, object, loop.expression);
                        if (loop.assignment.isPresent())
                            findVariable(loop.assignment.get().name, locals, object).Assign(exp);
                        while (exp.Value) {
                            interpretStatementBlock(object, loop.statements, locals);
                            exp = (BooleanIDT) evaluate(locals, object, loop.expression);
                            if (loop.assignment.isPresent())
                                findVariable(loop.assignment.get().name, locals, object).Assign(exp);
                        }
                    }
                    // throw new RuntimeException("Invalid loop condition");
                }
                default -> throw new RuntimeException("Unknown statement");
            }
        }
    }

    /**
     *  evaluate() processes everything that is an expression - math, variables, boolean expressions.
     *  There is a good bit of recursion in here, since math and comparisons have left and right sides that need to be evaluated.
     *
     * See the How To Write an Interpreter document for examples
     * For each possible ExpressionNode, do the work to resolve it:
     * BooleanLiteralNode - create a new BooleanLiteralNode with the same value
     *      - Same for all of the basic data types
     * BooleanOpNode - Evaluate() left and right, then perform either and/or on the results.
     * CompareNode - Evaluate() both sides. Do good comparison for each data type
     * MathOpNode - Evaluate() both sides. If they are both numbers, do the math using the built-in operators. Also handle String + String as concatenation (like Java)
     * MethodCallExpression - call doMethodCall() and return the first value
     * VariableReferenceNode - call findVariable()
     * @param locals the local variables
     * @param object - the current object we are running
     * @param expression - some expression to evaluate
     * @return a value
     */
    private InterpreterDataType evaluate(HashMap<String, InterpreterDataType> locals, Optional<ObjectIDT> object, ExpressionNode expression) {
        if (expression instanceof NumericLiteralNode nl)
            return new NumberIDT(nl.value);
        if (expression instanceof BooleanLiteralNode bl)
            return new BooleanIDT(bl.value);
        if (expression instanceof StringLiteralNode sl)
            return new StringIDT(sl.value);

        if (expression instanceof BooleanOpNode bop) {
            BooleanIDT left = (BooleanIDT)evaluate(locals, object, bop.left);
            BooleanIDT right = (BooleanIDT)evaluate(locals, object, bop.right);
            switch (bop.op) {
                case and: return new BooleanIDT(left.Value && right.Value);
                case or:  return new BooleanIDT(left.Value || right.Value);
            }
        }
        if (expression instanceof CompareNode c) {
            NumberIDT left = (NumberIDT)evaluate(locals, object, c.left);
            NumberIDT right = (NumberIDT)evaluate(locals, object, c.right);
            switch (c.op) {
                case eq: return new BooleanIDT(left.Value == right.Value);
                case ne: return new BooleanIDT(left.Value != right.Value);
                case lt: return new BooleanIDT(left.Value < right.Value);
                case gt: return new BooleanIDT(left.Value > right.Value);
                case le: return new BooleanIDT(left.Value <= right.Value);
                case ge: return new BooleanIDT(left.Value >= right.Value);
            }
        }
        if (expression instanceof MathOpNode mop) {
            InterpreterDataType left = evaluate(locals, object, mop.left);
            InterpreterDataType right = evaluate(locals, object, mop.right);
            if (left instanceof StringIDT l && right instanceof StringIDT r) {
                if (mop.op == MathOpNode.MathOperations.add)
                    return new StringIDT(l.Value + r.Value);
                throw new RuntimeException("Can only perform operation '+' for string types");
            }
            if (left instanceof NumberIDT l && right instanceof NumberIDT r) {
                switch (mop.op) {
                    case add: return new NumberIDT(l.Value + r.Value);
                    case subtract: return new NumberIDT(l.Value - r.Value);
                    case multiply: return new NumberIDT(l.Value * r.Value);
                    case divide:   return new NumberIDT(l.Value / r.Value);
                }
            }
            throw new RuntimeException("Operation '" + mop.op + "' not supported");
        }
        if (expression instanceof NotOpNode n) {
            BooleanIDT b = (BooleanIDT) evaluate(locals, object, n.left);
            b.Value = !b.Value;
            return b;
        }
        if (expression instanceof VariableReferenceNode vr)
            return findVariable(vr.name, locals, object);
        if (expression instanceof MethodCallExpressionNode mc) {
            MethodCallStatementNode mcs = new MethodCallStatementNode();
            mcs.objectName = mc.objectName;
            mcs.methodName = mc.methodName;
            mcs.parameters = mc.parameters;
            return findMethodForMethodCallAndRunIt(object, locals, mcs).getFirst();
        }
        if (expression instanceof NewNode n) {
            MethodCallStatementNode mcs = new MethodCallStatementNode();
            mcs.parameters = n.parameters;
            mcs.methodName = n.className;
            mcs.objectName = Optional.empty();
            Optional<ClassNode> cls = getClassByName(n.className);
            if (cls.isEmpty())
                throw new RuntimeException("Class not found");
            ObjectIDT obj = new ObjectIDT(cls.get());
            for (MemberNode member : cls.get().members)
                obj.members.put(member.declaration.name, instantiate(member.declaration.type));
            findConstructorAndRunIt(object, locals, mcs, obj);
            return obj;
        }
        throw new IllegalArgumentException();
    }

    //              Utility Methods

    /**
     * Used when trying to find a match to a method call. Given a method
     * declaration, does it match this method call?
     * <p>
     * We double-check with the parameters, too, although in theory JUST
     * checking the declaration to the call should be enough.
     * <p>
     * Match names, parameter counts (both declared count vs method call and
     * declared count vs value list), return counts.
     * <p>
     * If all of those match, consider the types (use TypeMatchToIDT).
     * <p>
     * If everything is OK, return true, else return false.
     * <p>
     * Note - if m is a built-in and isVariadic is true, skip all of the
     * parameter validation.
     * @param m - the method declaration we are considering
     * @param mc - the method call we are trying to match
     * @param parameters - the parameter values for this method call
     * @return does this method match the method call?
     */
    private boolean doesMatch(MethodDeclarationNode m, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {
        if (m instanceof BuiltInMethodDeclarationNode)
            return true;
        if (!m.name.equals(mc.methodName))
            return false;
        if (m.parameters.size() != mc.parameters.size())
            return false;
        if (m.parameters.size() != parameters.size())
            return false;
        if (m.returns.size() != mc.returnValues.size())
            return false;
        for (int i = 0; i < parameters.size(); i++) {
            if (!typeMatchToIDT(m.parameters.get(i).type, parameters.get(i)))
                return false;
        }
        return true;
    }

    /**
     * Very similar to DoesMatch() except simpler - there are no return values, the name will always match.
     * @param c - a particular constructor
     * @param mc - the method call
     * @param parameters - the parameter values
     * @return does this constructor match the method call?
     */
    private boolean doesConstructorMatch(ConstructorNode c, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {
        if (c.parameters.size() != mc.parameters.size())
            return false;
        if (c.parameters.size() != parameters.size())
            return false;
        for (int i = 0; i < parameters.size(); i++) {
            if (!typeMatchToIDT(c.parameters.get(i).type, parameters.get(i)))
                return false;
        }
        return true;
    }

    /**
     * Used when we call a method to get the list of values for the parameters.
     *
     * for each parameter in the method call, call Evaluate() on the parameter to get an IDT and add it to a list
     * @param object - the current object
     * @param locals - the local variables
     * @param mc - a method call
     * @return the list of method values
     */
    private List<InterpreterDataType> getParameters(Optional<ObjectIDT> object, HashMap<String,InterpreterDataType> locals, MethodCallStatementNode mc) {
        LinkedList<InterpreterDataType> values = new LinkedList<>();
        for (ExpressionNode exp : mc.parameters)
            values.add(evaluate(locals, object, exp));
        return values;
    }

    /**
     * Used when we have an IDT and we want to see if it matches a type definition
     * Commonly, when someone is making a function call - do the parameter values match the method declaration?
     *
     * If the IDT is a simple type (boolean, number, etc) - does the string type match the name of that IDT ("boolean", etc)
     * If the IDT is an object, check to see if the name matches OR the class has an interface that matches
     * If the IDT is a reference, check the inner (refered to) type
     * @param type the name of a data type (parameter to a method)
     * @param idt the IDT someone is trying to pass to this method
     * @return is this OK?
     */
    private boolean typeMatchToIDT(String type, InterpreterDataType idt) {
        switch (idt) {
            case BooleanIDT b: return type.equals("boolean");
            case NumberIDT  n: return type.equals("number");
            case StringIDT  s: return type.equals("string");
            case CharIDT    c: return type.equals("character");
            case ObjectIDT  o:
                if (type.equals(o.astNode.name))
                    return true;
                for (String interfaceName : o.astNode.interfaces)
                    if (type.equals(interfaceName))
                        return true;
                break;
            case ReferenceIDT r:
                if (r.refersTo.isPresent())
                    return type.equals(r.refersTo.get().astNode.name);
                break;
            default:
                throw new RuntimeException("Unable to resolve type " + type);
        }
        return false;
//        switch (type) {
//            case "number": return (idt instanceof NumberIDT);
//            case "string": return (idt instanceof StringIDT);
//            case "boolean": return (idt instanceof BooleanIDT);
//            case "character": return (idt instanceof CharIDT);
//        }
//        if (idt instanceof ReferenceIDT ref && ref.refersTo.isPresent())
//            return ref.refersTo.get().astNode.name.equals(type);
//        throw new RuntimeException("Unable to resolve type " + type);
    }

    /**
     * Find a method in an object that is the right match for a method call
     * (same name, parameters match, etc. Uses doesMatch() to do most of the
     * work)
     *
     * Given a method call, we want to loop over the methods for that class,
     * looking for a method that matches (use DoesMatch) or throw.
     * @param object - an object that we want to find a method on
     * @param mc - the method call
     * @param parameters - the parameter value list
     * @return a method or throws an exception
     */
    private MethodDeclarationNode getMethodFromObject(ObjectIDT object, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {
        for (MethodDeclarationNode m : object.astNode.methods)
            if (doesMatch(m, mc, parameters))
                return m;
        throw new RuntimeException("Unable to resolve method call " + mc);
    }

    /**
     * Find a class, given the name. Just loops over the TranNode's classes member, matching by name.
     *
     * Loop over each class in the top node, comparing names to find a match.
     * @param name Name of the class to find
     * @return either a class node or empty if that class doesn't exist
     */
    private Optional<ClassNode> getClassByName(String name) {
        for (ClassNode c : top.Classes)
            if (c.name.equals(name))
                return Optional.of(c);
        return Optional.empty();
    }

    /**
     * Given an execution environment (the current object, the current local
     * variables), find a variable by name.
     *
     * @param name  - the variable that we are looking for
     * @param locals - the current method's local variables
     * @param object - the current object (so we can find members)
     * @return the IDT that we are looking for or throw an exception
     */
    private InterpreterDataType findVariable(String name, HashMap<String,InterpreterDataType> locals, Optional<ObjectIDT> object) {
        InterpreterDataType variable;
        variable = locals.get(name);
        if (variable != null)
            return variable;
        if (object.isPresent()) {
            variable = object.get().members.get(name);
            if (variable != null)
                return variable;
        }
        throw new RuntimeException("Unable to find variable '" + name + "'. Was it declared?");
    }

    /**
     * Given a string (the type name), make an IDT for it.
     *
     * @param type The name of the type (string, number, boolean, character). Defaults to ReferenceIDT if not one of those.
     * @return an IDT with default values (0 for number, "" for string, false for boolean, ' ' for character)
     */
    private InterpreterDataType instantiate(String type) {
        switch (type) {
            case "number": return new NumberIDT(0);
            case "string": return new StringIDT("");
            case "boolean": return new BooleanIDT(false);
            case "character": return new CharIDT(' ');
        }
        Optional<ClassNode> cls = getClassByName(type);
        if (cls.isEmpty())
            throw new RuntimeException("Unknown type '" + type + "'");
        ObjectIDT object = new ObjectIDT(cls.get());
        ReferenceIDT ref = new ReferenceIDT();
        ref.refersTo = Optional.of(object);
        return ref;
    }
}
