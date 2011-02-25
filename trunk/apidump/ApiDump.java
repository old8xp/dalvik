/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package apidump;

import com.google.inject.TypeLiteral;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Writes a plain text report describing the API of a set of packages.
 */
public final class ApiDump {

    private static final Comparator<TypeLiteral<?>> ORDER_TYPES = new Comparator<TypeLiteral<?>>() {
        public int compare(TypeLiteral<?> a, TypeLiteral<?> b) {
            return a.toString().compareTo(b.toString());
        }
    };

    private static final Comparator<Class<?>> ORDER_CLASSES = new Comparator<Class<?>>() {
        public int compare(Class<?> a, Class<?> b) {
            return a.getName().compareTo(b.getName());
        }
    };

    /**
     * Order members by member type (fields, constructors, then methods), name, and parameters.
     */
    private static final Comparator<QualifiedMember> ORDER_MEMBERS = new Comparator<QualifiedMember>() {
        public int compare(QualifiedMember a, QualifiedMember b) {
            int mt = a.rankByType() - b.rankByType();
            if (mt != 0) {
                return mt;
            }

            int n = a.member.getName().compareTo(b.member.getName());
            if (n != 0) {
                return n;
            }

            if (a.isField()) {
                return 0;
            }

            List<TypeLiteral<?>> aParameters = a.getParameterTypes();
            List<TypeLiteral<?>> bParameters = b.getParameterTypes();

            for (int i = 0; i < aParameters.size() && i < bParameters.size(); i++) {
                int t = ORDER_TYPES.compare(aParameters.get(i), bParameters.get(i));
                if (t != 0) {
                    return t;
                }
            }

            return aParameters.size() - bParameters.size();
        }
    };

    private static final TypeLiteral<Object> OBJECT = new TypeLiteral<Object>() {};
    private final Set<QualifiedMember> MEMBERS_TO_SUPPRESS = computeMembersToSuppress();

    private final TreeSet<Class<?>> classes = new TreeSet<Class<?>>(ORDER_CLASSES);
    private final boolean grepFormat;
    private final boolean includeInherited;
    private final PrintStream out;

    public ApiDump(boolean grepFormat, boolean includeInherited, PrintStream out) {
        this.grepFormat = grepFormat;
        this.includeInherited = includeInherited;
        this.out = out;
    }

    public boolean isVisible(int modifiers) {
        return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
    }

    private void dump() {
        for (Class<?> type : classes) {
            dumpType(type);
        }
    }

    private void dumpType(Class<?> type) {
        if (!isVisible(type.getModifiers())) {
            return;
        }

        dumpClassDeclaration(type);

        Set<Class<?>> visited = new TreeSet<Class<?>>(ORDER_CLASSES);
        Set<QualifiedMember> members = new TreeSet<QualifiedMember>(ORDER_MEMBERS);
        getMembersRecursive(TypeLiteral.get(type), visited, members, true);

        for (QualifiedMember member : members) {
            if (!MEMBERS_TO_SUPPRESS.contains(member)) {
                dumpMemberDeclaration(type, member);
            }
        }

        if (!grepFormat) {
            out.print("}\n");
        }
    }

    /**
     * Writes a type declaration like this:
     *
     * public class java.util.HashMap<K, V>
     *     extends java.util.AbstractMap<K, V>
     *     implements java.io.Serializable, java.lang.Cloneable, java.util.Map<K, V> {
     */
    private void dumpClassDeclaration(Class<?> rawType) {
        TypeLiteral<?> type = TypeLiteral.get(rawType);
        
        if (grepFormat) {
            out.print(rawType.getName());
            out.print("\t");
        }

        String supertypeListSeparator = grepFormat ? " " : "\n    ";

        dumpModifiers(rawType.getModifiers(), rawType.isEnum(),
                rawType.isEnum() || rawType.isInterface());
        out.print(" " + typeType(rawType));
        out.print(" " + type);

        TypeVariable<?>[] typeParameters = rawType.getTypeParameters();
        if (typeParameters.length > 0) {
            out.print("<" + join(", ", Arrays.asList(typeParameters)) + ">");
        }

        // TODO: skip private supertypes
        Class<?> rawSuperclass = rawType.getSuperclass();
        if (rawSuperclass != null && rawSuperclass != Object.class) {
            out.print(supertypeListSeparator);
            out.print("extends " + type.getSupertype(rawSuperclass));
        }

        Set<TypeLiteral<?>> allImplementedInterfaces = new TreeSet<TypeLiteral<?>>(ORDER_TYPES);
        getImplementedInterfaces(TypeLiteral.get(rawType), allImplementedInterfaces);
        if (!allImplementedInterfaces.isEmpty()) {
            out.print(supertypeListSeparator);
            out.print("implements " + join(", ", allImplementedInterfaces));
        }

        if (grepFormat) {
            out.print(" {}\n");
        } else {
            out.print(" {\n");
        }
    }

    private static String join(String delimiter, Iterable<?> args) {
        Iterator<?> i = args.iterator();
        if (!i.hasNext()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        result.append(i.next());
        while (i.hasNext()) {
            result.append(delimiter);
            result.append(i.next());
        }
        return result.toString();
    }

    /**
     * Prints a member declaration like these:
     *
     *   public HashMap(int, float);
     *   protected void finalize() throws java.lang.Throwable;
     * 
     * @param ofType the type this member is being dumped for. For inherited
     *     methods, this may differ from the member's declaring type which could
     *     be a supertype of this parameter.
     */
    private void dumpMemberDeclaration(Class<?> ofType, QualifiedMember member) {
        if (!isVisible(member.member.getModifiers())) {
            return;
        }
        
        if (grepFormat) {
            out.print(ofType.getName());
            out.print(".");
            out.print(member.member instanceof Constructor ? "<init>" : member.member.getName());
            out.print("\t");
        } else {
            out.print("  ");
        }

        Class<?> declaringClass = member.type.getRawType();
        dumpModifiers(member.member.getModifiers(), declaringClass.isEnum(),
                declaringClass.isEnum() || declaringClass.isInterface());

        if (member.isField()) {
            Field field = (Field) member.member;
            out.print(" " + member.type.getFieldType((Field) member.member));
            out.print(" " + field.getName());
            out.print(";\n");
            return;
        }

        List<TypeLiteral<?>> parameters = member.getParameterTypes();
        List<TypeLiteral<?>> exceptions = member.getExceptionTypes();
        TypeVariable<?>[] typeParameters;
        boolean isVarArgs;
        String name;
        if (member.member instanceof Constructor) {
            Constructor constructor = (Constructor) member.member;
            typeParameters = constructor.getTypeParameters();
            isVarArgs = constructor.isVarArgs();
            name = constructor.getName();
        } else if (member.member instanceof Method) {
            Method method = (Method) member.member;
            typeParameters = method.getTypeParameters();
            isVarArgs = method.isVarArgs();
            name = method.getName();
        } else {
            throw new AssertionError();
        }

        if (typeParameters.length > 0) {
            out.print(" <" + join(", ", Arrays.asList(typeParameters)) + ">");
        }

        if (member.member instanceof Method) {
            out.print(" " + member.type.getReturnType((Method) member.member));
        }

        out.print(" " + name);
        out.print("(");
        for (int i = 0; i < parameters.size(); i++) {
            TypeLiteral<?> parameter = parameters.get(i);
            if (i > 0) {
                out.print(", ");
            }
            out.print(parameterToString(parameter, (i == parameters.size() - 1 && isVarArgs)));
        }
        out.print(")");

        Set<TypeLiteral<?>> deduplicatedExceptions = deduplicateExceptions(exceptions);
        if (!deduplicatedExceptions.isEmpty()) {
            out.print(" throws " + join(", ", deduplicatedExceptions));
        }
        out.print(";\n");
    }

    /**
     * Removes unnecessary exceptions. Some members declare redundant exceptions,
     * for example:
     *     public void foo() throws IOException, FileNotFoundException;
     * could be simplified to:
     *     public void foo() throws IOException;
     */
    private Set<TypeLiteral<?>> deduplicateExceptions(List<TypeLiteral<?>> exceptions) {
        Set<TypeLiteral<?>> result = new TreeSet<TypeLiteral<?>>(ORDER_TYPES);

        eachException:
        for (TypeLiteral<?> exception : exceptions) {
            Class<?> rawException = exception.getRawType();
            if (RuntimeException.class.isAssignableFrom(rawException)
                    || Error.class.isAssignableFrom(rawException)) {
                continue;
            }
            for (Iterator<TypeLiteral<?>> i = result.iterator(); i.hasNext(); ) {
                TypeLiteral<?> existing = i.next();
                if (existing.getRawType().isAssignableFrom(rawException)) {
                    continue eachException;
                }
                if (rawException.isAssignableFrom(existing.getRawType())) {
                    i.remove();
                }
            }
            result.add(exception);
        }

        return result;
    }

    private void dumpModifiers(int modifiers, boolean omitFinal, boolean omitAbstract) {
        if (Modifier.isPublic(modifiers)) {
            out.print("public");
        } else if (Modifier.isProtected(modifiers)) {
            out.print("protected");
        } else if (Modifier.isPrivate(modifiers)) {
            out.print("private");
        } else {
            out.print("package");
        }
        if (Modifier.isStatic(modifiers)) {
            out.print(" static");
        }
        if (!omitFinal && Modifier.isFinal(modifiers)) {
            out.print(" final");
        }
        if (!omitAbstract && Modifier.isAbstract(modifiers)) {
            out.print(" abstract");
        }
    }

    private static String parameterToString(TypeLiteral<?> type, boolean isVarArgs) {
        String toString = type.toString();
        if (isVarArgs) {
            if (!toString.endsWith("[]")) {
                throw new IllegalArgumentException();
            }
            return toString.substring(0, toString.length() - 2) + "...";
        }
        return toString;
    }

    private String typeType(Class<?> type) {
        if (type.isEnum()) {
            return "enum";
        } else if (type.isAnnotation()) {
            return "@interface";
        } else if (type.isInterface()) {
            return "interface";
        } else {
            return "class";
        }
    }

    private void getImplementedInterfaces(TypeLiteral<?> type, Set<TypeLiteral<?>> sink) {
        // TODO: omit private interfaces and private superclasses

        Class<?> rawType = type.getRawType();
        for (Class<?> rawInterface : rawType.getInterfaces()) {
            TypeLiteral<?> implemented = type.getSupertype(rawInterface);
            if (sink.add(implemented)) {
                getImplementedInterfaces(implemented, sink);
            }
        }
        Class<?> rawSuperclass = rawType.getSuperclass();
        if (rawSuperclass != null) {
            getImplementedInterfaces(type.getSupertype(rawSuperclass), sink);
        }
    }

    private void getMembersRecursive(TypeLiteral<?> type, Set<Class<?>> visited,
            Set<QualifiedMember> sink, boolean direct) {
        // fields and constructors aren't inherited
        Class<?> rawType = type.getRawType();
        if (direct) {
            for (Constructor constructor : rawType.getDeclaredConstructors()) {
                sink.add(new QualifiedMember(type, constructor));
            }
            for (Field field : rawType.getDeclaredFields()) {
                sink.add(new QualifiedMember(type, field));
            }
        }

        for (Method method : rawType.getDeclaredMethods()) {
            QualifiedMember member = new QualifiedMember(type, method);
            if (method.isSynthetic()
                    || (!direct && Modifier.isStatic(method.getModifiers()))) {
                continue;
            }
            /*
             * Don't add a method when an override is already present. That could break covariant
             * return types.
             */
            if (!sink.contains(member)) {
                sink.add(member);
            }
        }
        
        if (includeInherited) {
            Class<?> rawSuperclass = rawType.getSuperclass();
            if (rawSuperclass != null) {
                if (visited.add(rawSuperclass)) {
                    getMembersRecursive(type.getSupertype(rawSuperclass), visited, sink, false);
                }
            }

            for (Class<?> rawInterface : rawType.getInterfaces()) {
                if (visited.add(rawInterface)) {
                    getMembersRecursive(type.getSupertype(rawInterface), visited, sink, false);
                }
            }
        }
    }

    private void addPackages(List<String> packages) throws IOException {
        ClassPathScanner scanner = new ClassPathScanner();
        System.err.println("Scanning " + scanner.getClassPath());

        for (String packageName : packages) {
            Set<Class<?>> types = scanner.scan(packageName).getTopLevelClassesRecursive();
            if (types.isEmpty()) {
                System.err.println("No classes found in " + packageName);
                continue;
            }

            for (Class<?> type : types) {
                getTypesRecursive(type, this.classes);
            }
        }
    }

    private void getTypesRecursive(Class<?> type, Set<Class<?>> sink) {
        if (sink.add(type)) {
            for (Class<?> inner : type.getClasses()) {
                getTypesRecursive(inner, sink);
            }
        }
    }

    /**
     * Don't print members inherited by every class, unless it's clone(). We
     * have to print clone() because it's eligible for covariant return classes,
     * and subclasses may expose a different signature.
     */
    private Set<QualifiedMember> computeMembersToSuppress() {
        TreeSet<QualifiedMember> result = new TreeSet<QualifiedMember>(ORDER_MEMBERS);
        getMembersRecursive(OBJECT, new TreeSet<Class<?>>(ORDER_CLASSES), result, true);
        try {
            result.remove(new QualifiedMember(OBJECT, Object.class.getDeclaredMethod("clone")));
        } catch (NoSuchMethodException e) {
            throw new AssertionError();
        }
        return Collections.unmodifiableSet(result);
    }

    static class QualifiedMember {
        private final TypeLiteral<?> type;
        private final Member member;

        QualifiedMember(TypeLiteral<?> type, Member member) {
            this.type = type;
            this.member = member;
        }

        public boolean isField() {
            return member instanceof Field;
        }

        public List<TypeLiteral<?>> getParameterTypes() {
            return type.getParameterTypes(member);
        }

        public List<TypeLiteral<?>> getExceptionTypes() {
            return type.getExceptionTypes(member);
        }

        private int rankByType() {
            if (member instanceof Field) {
                return 0;
            } else if (member instanceof Constructor) {
                return 1;
            } else if (member instanceof Method) {
                return 2;
            } else {
                throw new AssertionError();
            }
        }

        @Override public boolean equals(Object o) {
            return o instanceof QualifiedMember
                    && ((QualifiedMember) o).type.equals(type)
                    && ((QualifiedMember) o).member.equals(member);
        }

        @Override public int hashCode() {
            return type.hashCode() ^ member.hashCode();
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Usage: ApiDump [options] <package names...>");
            System.out.println();
            System.out.println("Options:");
            System.out.println("  --grep-format: include type name on every line");
        }
        
        boolean grepFormat = false;
        boolean includeInherited = false;
        
        List<String> argsList = new ArrayList<String>();
        argsList.addAll(Arrays.asList(args));

        for (Iterator<String> i = argsList.iterator(); i.hasNext(); ) {
            String arg = i.next();
            if (arg.equals("--grep-format")) {
                grepFormat = true;
                i.remove();
            } else if (arg.equals("--include-inherited")) {
                includeInherited = true;
                i.remove();
            }
        }

        ApiDump dump = new ApiDump(grepFormat, includeInherited, System.out);
        dump.addPackages(argsList);
        dump.dump();
    }
}
