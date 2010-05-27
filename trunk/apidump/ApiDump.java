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

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * Writes a plain text report describing the API of a set of packages.
 */
public class ApiDump {

    public static class Covariant {
        public Covariant clone() {
            return null;
        }
    }

    private static final Comparator<Class<?>> ORDER_TYPES = new Comparator<Class<?>>() {
        public int compare(Class<?> a, Class<?> b) {
            return typeToString(a).compareTo(typeToString(b));
        }
    };

    /**
     * Order members by member type (fields, constructors, then methods), name, and parameters.
     */
    private static final Comparator<Member> ORDER_MEMBERS = new Comparator<Member>() {
        public int compare(Member a, Member b) {
            int mt = rankMemberByMemberType(a) - rankMemberByMemberType(b);
            if (mt != 0) {
                return mt;
            }

            int n = a.getName().compareTo(b.getName());
            if (n != 0) {
                return n;
            }

            Class<?>[] aParameters;
            Class<?>[] bParameters;
            if (a instanceof Constructor) {
                aParameters = ((Constructor) a).getParameterTypes();
                bParameters = ((Constructor) b).getParameterTypes();
            } else if (b instanceof Method) {
                aParameters = ((Method) a).getParameterTypes();
                bParameters = ((Method) b).getParameterTypes();
            } else {
                return 0;
            }

            for (int i = 0; i < aParameters.length && i < bParameters.length; i++) {
                int t = ORDER_TYPES.compare(aParameters[i], bParameters[i]);
                if (t != 0) {
                    return t;
                }
            }

            return aParameters.length - bParameters.length;
        }

        private int rankMemberByMemberType(Member member) {
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
    };

    /**
     * Don't print members inherited by every class, unless it's clone(). We
     * have to print clone() because it's eligible for covariant return types,
     * and subclasses may expose a different signature.
     */
    private static Set<Member> membersToSuppress = new TreeSet<Member>(ORDER_MEMBERS);
    static {
        getMembersRecursive(
                Object.class, new TreeSet<Class<?>>(ORDER_TYPES), membersToSuppress, true);
        try {
            membersToSuppress.remove(Object.class.getDeclaredMethod("clone"));
        } catch (NoSuchMethodException e) {
            throw new AssertionError();
        }
    }

    private final TreeSet<Class<?>> types = new TreeSet<Class<?>>(ORDER_TYPES);
    private final PrintStream out;

    public ApiDump(PrintStream out) {
        this.out = out;
    }

    public boolean isVisible(int modifiers) {
        return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
    }

    private void dump() {
        for (Class<?> type : types) {
            dumpType(type);
        }
    }

    private void dumpType(Class<?> type) {
        if (!isVisible(type.getModifiers())) {
            return;
        }

        dumpTypeDeclaration(type);

        Set<Class<?>> visited = new TreeSet<Class<?>>(ORDER_TYPES);
        Set<Member> members = new TreeSet<Member>(ORDER_MEMBERS);
        getMembersRecursive(type, visited, members, true);

        for (Member member : members) {
            if (!membersToSuppress.contains(member)) {
                dumpMemberDeclaration(member);
            }
        }

        out.print("}\n");
    }

    /**
     * Writes a type declaration like this:
     *
     * public class java.util.HashMap
     *     extends java.util.AbstractMap
     *     implements java.io.Serializable, java.lang.Cloneable, java.util.Map {
     */
    private void dumpTypeDeclaration(Class<?> type) {
        dumpModifiers(type.getModifiers());
        out.print(" " + typeType(type));
        out.print(" " + typeToString(type));

        Class<?> superClass = type.getSuperclass(); // TODO: skip private supertypes
        if (superClass != null && superClass != Object.class) {
            out.print("\n    extends " + typeToString(superClass));
        }

        Set<Class<?>> allImplementedInterfaces = new TreeSet<Class<?>>(ORDER_TYPES);
        getImplementedInterfaces(type, allImplementedInterfaces);
        if (!allImplementedInterfaces.isEmpty()) {
            Iterator<Class<?>> i = allImplementedInterfaces.iterator();
            out.print("\n    implements " + i.next());
            while (i.hasNext()) {
                out.print(", " + i.next());
            }
        }

        out.print(" {\n");
    }

    /**
     * Prints a member declaration like this:
     *   public HashMap(int, float);
     *   protected void finalize() throws java.lang.Throwable;
     */
    private void dumpMemberDeclaration(Member member) {
        if (!isVisible(member.getModifiers())) {
            return;
        }

        out.print("  ");
        dumpModifiers(member.getModifiers());

        if (member instanceof Field) {
            Field field = (Field) member;
            out.print(" " + typeToString(field.getType()));
            out.print(" " + field.getName());
            out.print(";\n");
            return;
        }

        Class<?>[] parameters;
        Class<?>[] exceptions;
        if (member instanceof Constructor) {
            Constructor constructor = (Constructor) member;
            parameters = constructor.getParameterTypes();
            exceptions = constructor.getExceptionTypes();
            out.print(" " + constructor.getName());
        } else if (member instanceof Method) {
            Method method = (Method) member;
            parameters = method.getParameterTypes();
            exceptions = method.getExceptionTypes();
            out.print(" " + typeToString(method.getReturnType()));
            out.print(" " + method.getName());
        } else {
            throw new AssertionError();
        }

        out.print("(");
        int count = 0;
        for (Class<?> parameter : parameters) {
            if (count++ > 0) {
                out.print(", ");
            }
            out.print(typeToString(parameter));
        }
        out.print(")");

        count = 0;
        for (Class<?> exception : exceptions) {
            if (isChecked(exception)) {
                if (count++ == 0) {
                    out.print(" throws ");
                } else {
                    out.print(", ");
                }
                out.print(typeToString(exception));
            }
        }

        out.print(";\n");
    }

    private boolean isChecked(Class<?> exception) {
        return !RuntimeException.class.isAssignableFrom(exception)
                && !Error.class.isAssignableFrom(exception);
    }

    private void dumpModifiers(int modifiers) {
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
        if (Modifier.isFinal(modifiers)) {
            out.print(" final");
        }
        if (Modifier.isAbstract(modifiers)) {
            out.print(" abstract");
        }
    }

    private static String typeToString(Class<?> type) {
        if (type.isArray()) {
            return typeToString(type.getComponentType()) + "[]";
        } else {
            return type.getName();
        }
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

    private void getImplementedInterfaces(Class<?> type, Set<Class<?>> sink) {
        for (Class<?> implemented : type.getInterfaces()) {
             // TODO: skip private interfaces
            if (sink.add(implemented)) {
                getImplementedInterfaces(implemented, sink);
            }
        }
    }

    private static void getMembersRecursive(Class<?> type, Set<Class<?>> visited, Set<Member> sink,
            boolean direct) {
        // fields and constructors aren't inherited, but methods are 
        if (direct) {
            sink.addAll(Arrays.asList(type.getDeclaredConstructors()));
            sink.addAll(Arrays.asList(type.getDeclaredFields()));
        }
        sink.addAll(Arrays.asList(type.getDeclaredMethods()));

        Class<?> superClass = type.getSuperclass();
        if (superClass != null) {
            if (visited.add(superClass)) {
                getMembersRecursive(superClass, visited, sink, false);
            }
        }

        for (Class<?> implemented : type.getInterfaces()) {
            if (visited.add(implemented)) {
                getMembersRecursive(implemented, visited, sink, false);
            }
        }
    }


    private void addPackages(String... packages) throws IOException {
        ClassPathScanner scanner = new ClassPathScanner(ApiDump.class.getClassLoader());
        for (String p : packages) {
            Set<Class<?>> types = scanner.scan(p).getTopLevelClassesRecursive();
            for (Class<?> type : types) {
                getTypesRecursive(type, types);
            }
        }
    }

    private void getTypesRecursive(Class<?> type, Set<Class<?>> sink) {
        this.types.add(type);
        for (Class<?> inner : type.getClasses()) {
            getTypesRecursive(inner, sink);
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Usage: ApiDump <packages...>");
        }

        ApiDump dump = new ApiDump(System.out);
        dump.addPackages(args);
        dump.dump();
    }
}
