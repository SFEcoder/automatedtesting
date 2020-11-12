/**
 * 对 CGNode 的替代，只保留了方法签名和类的内部信息
 * @Author: zzx
 * @Date: 2020/11/11 17:05
 */
public class Node {

    // 类的内部表示
    private String classInnerName;
    // 方法签名
    private String signature;

    public Node(String classInnerName,String signature){
        this.classInnerName = classInnerName;
        this.signature = signature;
    }

    public void setClassInnerName(String classInnerName){
        this.classInnerName=classInnerName;
    }

    public void  setSignature(String signature){
        this.signature = signature;
    }

    public String getClassInnerName(){
        return classInnerName;
    }

    public String getSignature(){
        return signature;
    }


    /**
     * 用來判定Node 是否相同
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj){
        if(obj instanceof Node){
            return this.getAllName().equals(((Node)obj).getAllName());
        }else{
            return super.equals(obj);
        }
    }

    @Override
    public int hashCode(){
        return this.getAllName().hashCode();
    }

    /**
     * 用来判定节点的key
     * @return
     */
    public String getAllName(){
        return classInnerName+" "+signature;
    }


}
