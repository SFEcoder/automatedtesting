import org.junit.Assert;

import org.junit.Test;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @Author: zzx
 * @Date: 2020/11/11 20:23
 */
public class TestCase {


    @Test
    public void TestDataLog(){
        String[] args = {"-m", "Data/2-DataLog/Target/", "Data/2-DataLog/data/change_info.txt"};
        Set<String> result =  TestSelection.evaluate(args);
        assertResult("Data/2-DataLog/data/selection-method.txt", result);
        args[0] = "-c";
        result =  TestSelection.evaluate(args);
        assertResult("Data/2-DataLog/data/selection-class.txt", result);
    }


    @Test
    public void TestCMD(){
        String[] args ={"-m", "Data/0-CMD/Target/", "Data/0-CMD/data/change_info.txt"};
        Set<String> result = TestSelection.evaluate(args);
        assertResult("Data/0-CMD/data/selection-method.txt", result);
        args[0] = "-c";
        result =  TestSelection.evaluate(args);
        assertResult("Data/0-CMD/data/selection-class.txt", result);
    }

    @Test
    public void TestBinaryHeap(){
        String[] args ={"-m", "Data/3-BinaryHeap/Target/", "Data/3-BinaryHeap/data/change_info.txt"};
        Set<String> result =  TestSelection.evaluate(args);
        assertResult("Data/3-BinaryHeap/data/selection-method.txt", result);
        args[0] = "-c";
        result =  TestSelection.evaluate(args);
        assertResult("Data/3-BinaryHeap/data/selection-class.txt", result);
    }

    @Test
    public void TestALU(){
        String[] args = {"-m", "Data/1-ALU/Target/", "Data/1-ALU/data/change_info.txt"};
        Set<String> result =  TestSelection.evaluate(args);
        assertResult("Data/1-ALU/data/selection-method.txt", result);
        args[0] = "-c";
        result =  TestSelection.evaluate(args);
        assertResult("Data/1-ALU/data/selection-class.txt", result);
    }





    @Test
    public void TestNextDay(){
        String[] args = new String[]{"-m", "Data/4-NextDay/Target/", "Data/4-NextDay/data/change_info.txt"};
        Set<String> result =  TestSelection.evaluate(args);
        assertResult("Data/4-NextDay/data/selection-method.txt", result);
        args[0] = "-c";
        result =  TestSelection.evaluate(args);
        assertResult("Data/4-NextDay/data/selection-class.txt", result);
    }

    @Test
    public void TestNextMoreTriangle(){
        String[] args = new String[]{"-m", "Data/5-MoreTriangle/Target/", "Data/5-MoreTriangle/data/change_info.txt"};
        Set<String> result =  TestSelection.evaluate(args);
        assertResult("Data/5-MoreTriangle/data/selection-method.txt", result);
        args[0] = "-c";
        result =  TestSelection.evaluate(args);
        assertResult("Data/5-MoreTriangle/data/selection-class.txt", result);
    }

    private void assertResult(String path, Set<String> result) {
        try{
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
            String temp = null;
            List<String> expects =new ArrayList<String>();
            while ((temp = bufferedReader.readLine()) != null) {
                expects.add(new String(temp));
            }
            for(int i=0;i<expects.size();i++){
                String str = expects.get(i);
                if(result.contains(str)){
                    result.remove(str);
                }else{
                    if(str.equals("")){
                        expects.remove("");
                    }else{
                        System.out.println(str);
                        Assert.fail();
                    }
                }
            }
            Assert.assertEquals(result.size(), 0);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
