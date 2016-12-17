import util.DBUtil;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by yuandl on 2016-12-16.
 */
public class DBTest {

    public static void main(String[] args) {
//        testInsert();
        testQuery();
        testUpdate();
//        testDelete();
        testQuery();
//        query3();
    }

    /**
     * 查询
     */
    public static void query() {
        String where = "job = ?  AND salary = ? ";
        String[] whereArgs = new String[]{"clerk", "3000"};

        try {
            List<Map<String, Object>> list = DBUtil.query("emp_test", false, null, where, whereArgs, null, null, null, null);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 测试查询
     */
    private static void testQuery() {
        String sql = "SELECT * FROM emp_test;";
        try {
            List<Map<String, Object>> list = DBUtil.query(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * SQL注入问题
     */
    public static void query2() {
        String name = "'1' OR '1'='1'";
        String password = "'1' OR '1'='1'";

        String sql = "SELECT * FROM emp_test WHERE name = " + name + " and password = " + password;
        String where = "name = ?  AND password = ? ";
        String[] whereArgs = new String[]{name, password};

        try {
            DBUtil.query(sql);
            DBUtil.query("emp_test", false, null, where, whereArgs, null, null, null, null);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void query3() {
        Map<String,Object> whereMap=new HashMap<>();
        whereMap.put("salary","10000");
        try {
            DBUtil.query("emp_test",whereMap);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 测试更新
     */
    private static void testUpdate() {
//        Map<String, Object> map = new HashMap<>();
//        map.put("name", "测试更新");
//
//        Map<String, Object> whereMap = new HashMap<>();
//        whereMap.put("emp_id", "1013");
        Map<String, Object> map = new HashMap<>();
        map.put("password", "123456");
        try {
            int count = DBUtil.update("emp_test", map, null);
            System.out.println("count=" + count);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    /**
     * 测试插入
     */
    private static void testInsert() {
        Map<String, Object> map = new HashMap<>();
        map.put("emp_id", 1013);
        map.put("name", "JDBCUtil测试");
        map.put("job", "developer");
        map.put("salary", 10000);
        map.put("hire_date", new Date());
        try {
            int count = DBUtil.insert("emp_test", map);
            System.out.println("count=" + count);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 测试删除
     */
    private static void testDelete() {
        Map<String, Object> whereMap = new HashMap<>();
        whereMap.put("emp_id", 1013);
        whereMap.put("job", "developer");
        try {
            int count = DBUtil.delete("emp_test", whereMap);
            System.out.println("count=" + count);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
