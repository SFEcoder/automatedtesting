import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.config.AnalysisScopeReader;

import java.io.*;
import java.util.*;


/**
 *
 * @Author: zzx
 * @Date: 2020/11/11 16:17
 */
public class TestSelection {

    public static void main(String [] args){
        evaluate(args);
    }

    /**
     * 分析选择的主入口
     * @param args
     * @return
     */
    public static Set<String> evaluate(String[] args){
        String flag = args[0];
        String filePath = args[1];
        String changePath = args[2];
        //存放src下的全路径path
        ArrayList<String> srcList =new ArrayList<String>();
        //存放test-classes下的文件全路径path
        ArrayList<String> testList = new ArrayList<String>();
        ArrayList<String> changeMethods = new ArrayList<String>();
        try {
            //读取变更文件
            BufferedReader bufferedReader =new BufferedReader(new InputStreamReader(new FileInputStream(changePath)));
            String temp=null;
            while ((temp=bufferedReader.readLine())!=null){
                changeMethods.add(new String(temp));
            }
            //读取target文件夹下的文件
            init(filePath,srcList,testList);
            Set<String> ans = new HashSet<>();
            switch (flag){
                case "-m":
                    //方法级
                    ans = buildModel(srcList,testList,changeMethods,true);
                    break;
                case "-c":
                    //类级
                    ans = buildModel(srcList,testList,changeMethods,false);
                    break;
            }
            BufferedWriter bw = null;
            if(flag.equals("-m")){
                bw =  new BufferedWriter(new FileWriter("selection-method.txt"));
            }else if(flag.equals("-c")){
                bw = new BufferedWriter(new FileWriter("selection-class.txt"));
            }
            for (String str : ans) {
                bw.write(str+"\n");
            }
            bw.close();
            return ans;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * flag 为真表示使用方法级的分析，为假表示使用类级的分析
     * 结果返回受到影响的依赖
     * @param srcList
     * @param testList
     * @param changeMethods
     * @param flag
     * @return
     */
    private static Set<String> buildModel(List<String> srcList, List<String> testList, List<String> changeMethods, boolean flag) throws IOException, InvalidClassFileException, ClassHierarchyException, CancelException {

        Map<Node,HashSet<Node>> srcGraph = new HashMap<>();
        //收集测试方法的集合
        Set<Node> testClassSet = new HashSet<>();
        AnalysisScope scope = AnalysisScopeReader.readJavaScope("scope.txt", new File("exclusion.txt"), ClassLoader.getSystemClassLoader());
        for(String testClassPath:testList){
            scope.addClassFileToScope(ClassLoaderReference.Application,new File(testClassPath));
        }
        setTest(scope,testClassSet);
        //将类文件对象加入scope中
        for(String classpath:srcList){
            scope.addClassFileToScope(ClassLoaderReference.Application,new File(classpath));
        }
        buildGraph(scope,srcGraph);
        Map<String,Set<String>> dot=new HashMap<>();
        //生成Dot文件
        makeDot(srcGraph,dot,flag);
        //受到影响的测试类
        Set<String> ans =new HashSet<>();
        List<String> temp =new ArrayList<>();
        for(int i=0;i<changeMethods.size();i++){
            //根据方法/类级选择变更信息的类或者方法
            temp.add(changeMethods.get(i).split(" ")[flag?1:0]);
        }
        changeMethods=new ArrayList<>(temp);
        for(String change:changeMethods){
            findRelation(change,srcGraph,testClassSet,ans,flag);
        }
        return ans;
    }

    /**
     * 生成.dot文件
     * @param srcGraph
     * @param dot
     * @param flag
     */
    private static void makeDot(Map<Node, HashSet<Node>> srcGraph, Map<String, Set<String>> dot, boolean flag) {
        for(Node node:srcGraph.keySet()){
            String s1 =flag?node.getSignature():node.getClassInnerName();
            if(!dot.containsKey("\"" + s1 + "\"")){
                dot.put("\"" + s1 + "\"",new HashSet<String>());
            }
            for(Node next:srcGraph.get(node)){
                String s2 =flag?next.getSignature():next.getClassInnerName();
                dot.get("\"" + s1 + "\"").add("\"" + s2 + "\"");
            }
        }
        //输出DOt文件
//        try {
//            BufferedWriter out;
//            if(flag){
//                out =  new BufferedWriter(new FileWriter("method-CMD.dot"));
//            }else{
//                out = new BufferedWriter(new FileWriter("class-CMD.dot"));
//            }
//
//            for (String key : dot.keySet()) {
//                for (String value : dot.get(key)) {
//                    out.write(key + " -> " + value+";\n");
//                }
//
//            }
//            out.close();
//            System.out.println("文件创建成功！");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }


    /**
     * 根据变更信息找到有关联的测试用例并加入到最终结果中
     * BFS方式实现
     * @param change
     * @param srcGraph
     * @param testClassSet
     * @param ans
     * @param flag
     */
    private static void findRelation(String change, Map<Node, HashSet<Node>> srcGraph, Set<Node> testClassSet, Set<String> ans, boolean flag) {
        //用来记录访问过的节点
        Set<Node> visited =new HashSet<>();
        Queue<Node> queue =new LinkedList<>();
        for(Node key:srcGraph.keySet()){
            if(flag){
                //表示方法级的判断
                if(key.getSignature().equals(change)){
                    queue.add(key);
                }
            }else{
                if(key.getClassInnerName().equals(change)){
                    queue.add(key);
                }
            }
        }
        while (!queue.isEmpty()){
            int sz =queue.size();
            //逐层遍历
            for(int i=0;i<sz;i++){
                Node node =queue.poll();
                if(visited.contains(node)){
                    continue;
                }
                visited.add(node);
                if(srcGraph.containsKey(node)){
                    Set<Node> set=srcGraph.get(node);
                    for(Node temp:set){
                        if(visited.contains(temp)){
                            continue;
                        }
                        queue.add(temp);
                        //判断temp节点是否属于测试方法并且为非初始化方法
                        if(flag){
                            if(testClassSet.contains(temp)&&temp.isTest()){
                                ans.add(temp.getAllName());
                            }
                        }else{
                            for(Node node1:testClassSet){
                                //在类级筛选层次上用类名做判断
                                if(temp.getClassInnerName().equals(node1.getClassInnerName())&&node1.isTest()){
                                    ans.add(node1.getAllName());
                                }
                            }
                        }

                    }
                }
            }
        }


    }

    /**
     * 生成所有测试方法的集合
     * @param scope
     * @param testSet
     * @throws ClassHierarchyException
     * @throws CancelException
     */
    public static  void setTest(AnalysisScope scope,Set<Node> testSet) throws ClassHierarchyException, CancelException {
        // 生成类层次关系对象
        ClassHierarchy cha = ClassHierarchyFactory.makeWithRoot(scope);
        // 生成进入点
        Iterable<Entrypoint> entrypoints = new AllApplicationEntrypoints(scope,cha);

        CHACallGraph cg =new CHACallGraph(cha);
        //初始化
        cg.init(entrypoints);

        for(CGNode node:cg){
            // 对ShrikeBTMethod 进行处理
            if(node.getMethod() instanceof ShrikeBTMethod){
                ShrikeBTMethod method =(ShrikeBTMethod) node.getMethod();
                if("Application".equals(method.getDeclaringClass().getClassLoader().toString())){
                    //获取声明该方法的类的内部表示
                    String classInnerName = method.getDeclaringClass().getName().toString();
                    //获取方法签名
                    String signature = method.getSignature();
                    //根据方法的注解是否含有Test来判断是否为一个测试方法
                    Node cur = new Node(classInnerName,signature,node.getMethod().getAnnotations().toString().contains("Test"));
                    testSet.add(cur);
                }
            }
        }
    }

    /**
     * 构建节点网络
     * @param scope
     * @param graph
     */
    public static  void buildGraph(AnalysisScope scope,Map<Node,HashSet<Node>> graph) throws ClassHierarchyException, CancelException {
        // 生成类层次关系对象
        ClassHierarchy cha = ClassHierarchyFactory.makeWithRoot(scope);
        // 生成进入点
        Iterable<Entrypoint> entrypoints = new AllApplicationEntrypoints(scope,cha);

        CHACallGraph cg =new CHACallGraph(cha);
        cg.init(entrypoints);

        for(CGNode node:cg){
            // 对ShrikeBTMethod 进行处理
            if(node.getMethod() instanceof ShrikeBTMethod){
                ShrikeBTMethod method =(ShrikeBTMethod) node.getMethod();
                if("Application".equals(method.getDeclaringClass().getClassLoader().toString())){
                    //获取声明该方法的类的内部表示
                    String classInnerName = method.getDeclaringClass().getName().toString();
                    //获取方法签名
                    String signature = method.getSignature();
                    Node cur = new Node(classInnerName,signature,node.getMethod().getAnnotations().toString().contains("Test"));
                    if(!graph.containsKey(cur)){
                        graph.put(cur,new HashSet<Node>());
                    }
                    //获取当前CGNode的所有后继并全部加入graph中
                    Iterator<CGNode> nodeIterator =cg.getPredNodes(node);
                    while (nodeIterator.hasNext()) {
                        CGNode next = nodeIterator.next();
                        if (next.getMethod() instanceof ShrikeBTMethod) {
                            ShrikeBTMethod tempMethod = (ShrikeBTMethod) next.getMethod();
                            if ("Application".equals(tempMethod.getDeclaringClass().getClassLoader().toString())) {
                                String nextClassInnerName = tempMethod.getDeclaringClass().getName().toString();
                                String nextSignature = tempMethod.getSignature();
                                Node follow = new Node(nextClassInnerName, nextSignature,next.getMethod().getAnnotations().toString().contains("Test"));
                                graph.get(cur).add(follow);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 读取target下的文件
     * @param filePath
     * @param srcList
     * @param testList
     */
    private static void init(String filePath, List<String> srcList, List<String> testList) {
        File parent = new File(filePath);
        if(parent.exists()){
            if(null == parent.listFiles()){
                return;
            }
            File [] children = parent.listFiles();
            for(File child: children){
                if(child.getName().equals("test-classes")){
                    addFile(child.listFiles(),testList);
                }else if(child.getName().equals("classes")){
                    addFile(child.listFiles(),srcList);
                }
            }
        }

    }

    /**
     * 读取父文件的子文件全路径名到targetFile中
     * @param fileList
     * @param targetFile
     */
    private static void addFile(File[] fileList, List<String> targetFile) {
        for(int i=0; i<fileList.length;i++){
            File file=fileList[i];
            if(file==null){
                continue;
            }
            if(file.isDirectory()){
                //判定为文件则进行递归加入
                addFile(file.listFiles(),targetFile);
            }else{
                targetFile.add(file.getPath());
            }
        }
    }
}




