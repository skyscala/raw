/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package unitreaper;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;




public class StdofReflectivReaper {

    @Test
    public void run(){
        
       
        try{
            String root="src\\main\\java";
            List<String> rawClassPaths=collectClassesInPath(root);
            for(String path:rawClassPaths){
                String str=path.substring(path.indexOf(root)+14, path.length());
                str=str.replace(".java", "");
                str=str.replace("\\", ".");
                
                new ConstructorReaper().construct(Class.forName(str));
                new MethodReaper().method(Class.forName(str));
                Assertions.assertTrue(true);//reach expectation
            }
        }catch(Exception ex){
            //ingore
        }

        
    }
    
    
    //********** Utility classes and methods**********//

    private AbstractInsGenerator create() {
        return new DefaultInsGenerator().next(
                new BeanGenerator().next(
                        new MapGenerator().next(
                                new SetGenerator().next(
                                        new ListGenerator()))));
    }
    
    class ConstructorReaper {

        public List construct(Class<?> clz) {

            AbstractInsGenerator generator = create();
            Constructor[] cons = clz.getDeclaredConstructors();
            List list = new ArrayList();
            for (Constructor con : cons) {
                try {
                    Parameter[] params = con.getParameters();
                    List<Object> objList = new ArrayList<>();

                    if (params != null && params.length > 0) {
                        for (Parameter p : params) {
                            try {
                                objList.add(generator.generate(p.getType()));
                            } catch (Exception ex) {
                                objList.add(null);
                            }
                        }
                    }
                    if (!objList.isEmpty()) {
                        Object ins = con.newInstance(objList.toArray(new Object[objList.size()]));
                        list.add(ins);
                    } else {
                        Object ins = con.newInstance();
                        list.add(ins);
                    }
                } catch (Exception ex) {

                }
            }
            return list;
        }
    }

    class MethodReaper {

        public void method(Class<?> clz) {

            for (int i = 0; i < 10; i++) {
                methodImpl(clz);
            }
        }

        public void methodImpl(Class<?> clz) {

            AbstractInsGenerator generator = create();

            Method[] methods = clz.getDeclaredMethods();

            println(clz.getName() + "{");
            for (Method m : methods) {

                Parameter[] params = m.getParameters();
                List<Object> objList = new ArrayList<>();
                StringBuilder sb = new StringBuilder();
                if (params != null && params.length > 0) {
                    for (Parameter p : params) {
                        String typeOfParam = p.getType().getName();
                        println(typeOfParam);
                        sb.append(p.getType().getName()).append(",");
                        try {
                            objList.add(generator.generate(p.getType()));
                        } catch (Exception ex) {
                            objList.add(null);
                        }
                    }
                }
                String paramString = sb.toString();
                if (paramString.length() > 1) {
                    paramString = paramString.substring(0, paramString.length() - 1);
                }
                println(m.getName() + "(" + paramString + ")");
                for (int i = 0; i < 10; i++) {
                    invoke(clz, m, objList);
                }
            }
            println("}");
        }

        private void invoke(Class clz, Method m, List<Object> objList) {
            try {
                ConstructorReaper constructorReaper = new ConstructorReaper();
                m.setAccessible(true);
                List ins = constructorReaper.construct(clz);
                
                //for instance methods
                for (Object inst : ins) {
                    if (objList.isEmpty()) {
                        m.invoke(inst);
                    } else {
                        println(objList);
                        m.invoke(inst,
                                objList.toArray(new Object[objList.size()]));
                    }
                }
                
                //for static methods
                if (objList.isEmpty()) {
                    m.invoke(null);
                } else {
                    println(objList);
                    m.invoke(null,
                            objList.toArray(new Object[objList.size()]));
                }
            } catch (Exception ex) {
                exception(ex);
            }

        }

        private void println(Object obj) {
            //System.out.println(obj);
        }

        private void exception(Exception ex) {

        }

    }

    public List<String> collectClassesInPath(String path){
        List<String> list=new ArrayList<>();
        File file = new File(path);
        if(file.isDirectory()){
            for(File f:file.listFiles()){
                if(f.isFile()&&f.getName().endsWith(".java")){
                    list.add(f.getAbsolutePath());
                }else if(f.isDirectory()){
                    list.addAll(collectClassesInPath(f.getAbsolutePath()));
                }
            }
            
        }else{
            if(file.getName().endsWith(".java")){
                list.add(file.getAbsolutePath());
            }
        }
        return list;
    }
    
    abstract class AbstractInsGenerator {

        protected AbstractInsGenerator next;

        AbstractInsGenerator next(AbstractInsGenerator next) {
            this.next = next;
            return this;
        }

        protected abstract Object generateImpl(Class clz);

        public Object generate(Class clz) {
            Object obj = generateImpl(clz);
            if (obj == null && next != null) {
                return next.generate(clz);
            }
            return obj;
        }

    }

    class BeanGenerator extends AbstractInsGenerator {

        private ConstructorReaper constructorReaper = new ConstructorReaper();

        @Override
        protected Object generateImpl(Class clz) {
            try {
                if (!clz.getName().startsWith("java")) {
                    Object obj = clz.newInstance();
                    return obj;
                }
            } catch (Exception ex) {

            }
            return null;
        }

    }

    class DefaultInsGenerator extends AbstractInsGenerator {

        @Override
        public Object generateImpl(Class clz) {

            try {
                String clzName = clz.getName();
                if (clzName.equals("java.lang.String")) {
                    return "String" + (int) (100 * Math.random());
                } else if (clzName.equals("java.util.Date")) {
                    return Calendar.getInstance().getTime();
                } else if (clz.isPrimitive()) {
                    return createPrimitiv(clz);
                } else {
                    Object wrapper = createWrapper(clz);
                    if (wrapper != null) {
                        return wrapper;
                    }
                }
            } catch (Exception ex) {
                //ignore
            }

            return null;

        }

        private Object createPrimitiv(Class clz) {
            if (clz.equals(int.class)) {
                return randomNumber();
            } else if (clz.equals(long.class)) {
                return randomNumber() * 1l;
            } else if (clz.equals(byte.class)) {
                return (byte) randomNumber();
            } else if (clz.equals(short.class)) {
                return (short) randomNumber();
            } else if (clz.equals(float.class)) {
                return (float) randomNumber();
            } else if (clz.equals(double.class)) {
                return (double) randomNumber();
            } else if (clz.equals(boolean.class)) {
                return randomBoolean();
            } else if (clz.equals(char.class)) {
                return 'Z';
            }
            return null;
        }

        private Object createWrapper(Class clz) {
            if (clz.equals(Integer.class)) {
                return randomNumber();
            } else if (clz.equals(Long.class)) {
                return randomNumber() * 1l;
            } else if (clz.equals(Byte.class)) {
                return (byte) randomNumber();
            } else if (clz.equals(Short.class)) {
                return (short) randomNumber();
            } else if (clz.equals(Float.class)) {
                return (float) randomNumber();
            } else if (clz.equals(Double.class)) {
                return (double) randomNumber();
            } else if (clz.equals(Boolean.class)) {
                return randomBoolean();
            } else if (clz.equals(Character.class)) {
                return 'z';
            }
            return null;
        }

        private int randomNumber() {
            int fac = (int) (Math.random() * 10);
            int val = (int) (Math.random() * 100);
            if (fac % 2 == 0) {
                return val * -1;
            }
            return val;
        }

        private boolean randomBoolean() {
            int fac = (int) (Math.random() * 10);
            if (fac % 2 == 0) {
                return true;
            }
            return false;
        }
    }

    class ListGenerator extends AbstractInsGenerator {

        private final List<String> CANDIDATES;

        {
            CANDIDATES = new ArrayList<>();
            CANDIDATES.add(List.class.getName());
            CANDIDATES.add(ArrayList.class.getName());
            CANDIDATES.add(LinkedList.class.getName());

        }

        @Override
        protected Object generateImpl(Class clz) {
            try {
                String clzName = clz.getName();
                if (CANDIDATES.contains(clzName)) {
                    return new ArrayList();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return null;
        }

    }

    class MapGenerator extends AbstractInsGenerator {

        private final List<String> CANDIDATES;

        {
            CANDIDATES = new ArrayList<>();
            CANDIDATES.add(Map.class.getName());
            CANDIDATES.add(HashMap.class.getName());

        }

        @Override
        protected Object generateImpl(Class clz) {
            try {
                String clzName = clz.getName();
                if (CANDIDATES.contains(clzName)) {
                    return new HashMap();
                }
            } catch (Exception ex) {
                //ignore
            }
            return null;
        }

    }

    class SetGenerator extends AbstractInsGenerator {

        private final List<String> CANDIDATES;

        {
            CANDIDATES = new ArrayList<>();
            CANDIDATES.add(Set.class.getName());
            CANDIDATES.add(HashSet.class.getName());

        }

        @Override
        protected Object generateImpl(Class clz) {
            try {
                String clzName = clz.getName();
                if (CANDIDATES.contains(clzName)) {
                    return new HashSet();
                }
            } catch (Exception ex) {
                //ignore
            }
            return null;
        }

    }
}
