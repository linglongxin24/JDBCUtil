#玩转JDBC打造数据库操作万能工具类JDBCUtil，加入了高效的数据库连接池，利用了参数绑定有效防止SQL注入

>在之前学习了MySQL和Oracle之后，那么，如和在Java种去连接这两种数据库。在这个轻量级的工具类当中，使用了数据库连接池
去提高数据库连接的高效性，并且使用了PreparedStatement来执行对SQL的预编译，能够有效防止SQL注入问题。

#一.准备工作:配置数据库连接属性文件：在项目新建config包下建立jdbc-mysql.properties并加入以下配置

```prooerties
jdbc.driverClassName=com.mysql.jdbc.Driver
jdbc.url=jdbc:mysql://127.0.0.1:3306/test?characterEncoding=utf8
jdbc.username=root
jdbc.password=root
```

#二.开始工作：数据库连接池对象，单例

```java
package util;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;

import java.beans.PropertyVetoException;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * 数据库连接对象
 * Created by yuandl on 2016-12-16.
 */
public class DBConnectionPool {
    private static volatile DBConnectionPool dbConnection;
    private ComboPooledDataSource cpds;

    /**
     * 在构造函数初始化的时候获取数据库连接
     */
    private DBConnectionPool() {
        try {
            /**通过属性文件获取数据库连接的参数值**/
            Properties properties = new Properties();
            FileInputStream fileInputStream = new FileInputStream("src/config/jdbc-mysql.properties");
            properties.load(fileInputStream);
            /**获取属性文件中的值**/
            String driverClassName = properties.getProperty("jdbc.driverClassName");
            String url = properties.getProperty("jdbc.url");
            String username = properties.getProperty("jdbc.username");
            String password = properties.getProperty("jdbc.password");

            /**数据库连接池对象**/
            cpds = new ComboPooledDataSource();

            /**设置数据库连接驱动**/
            cpds.setDriverClass(driverClassName);
            /**设置数据库连接地址**/
            cpds.setJdbcUrl(url);
            /**设置数据库连接用户名**/
            cpds.setUser(username);
            /**设置数据库连接密码**/
            cpds.setPassword(password);

            /**初始化时创建的连接数,应在minPoolSize与maxPoolSize之间取值.默认为3**/
            cpds.setInitialPoolSize(3);
            /**连接池中保留的最大连接数据.默认为15**/
            cpds.setMaxPoolSize(10);
            /**当连接池中的连接用完时，C3PO一次性创建新的连接数目;**/
            cpds.setAcquireIncrement(1);
            /**隔多少秒检查所有连接池中的空闲连接,默认为0表示不检查;**/
            cpds.setIdleConnectionTestPeriod(60);
            /**最大空闲时间,超过空闲时间的连接将被丢弃.为0或负数据则永不丢弃.默认为0;**/
            cpds.setMaxIdleTime(3000);

            /**因性能消耗大请只在需要的时候使用它。如果设为true那么在每个connection提交的
             时候都将校验其有效性。建议使用idleConnectionTestPeriod或automaticTestTable
             等方法来提升连接测试的性能。Default: false**/
            cpds.setTestConnectionOnCheckout(true);

            /**如果设为true那么在取得连接的同时将校验连接的有效性。Default: false **/
            cpds.setTestConnectionOnCheckin(true);
            /**定义在从数据库获取新的连接失败后重复尝试获取的次数，默认为30;**/
            cpds.setAcquireRetryAttempts(30);
            /**两次连接中间隔时间默认为1000毫秒**/
            cpds.setAcquireRetryDelay(1000);
            /** 获取连接失败将会引起所有等待获取连接的线程异常,
             但是数据源仍有效的保留,并在下次调用getConnection()的时候继续尝试获取连接.如果设为true,
             那么尝试获取连接失败后该数据源将申明已经断开并永久关闭.默认为false**/
            cpds.setBreakAfterAcquireFailure(true);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (PropertyVetoException e) {
            e.printStackTrace();
        }

    }

    /**
     * 获取数据库连接对象，单例
     *
     * @return
     */
    public static DBConnectionPool getInstance() {
        if (dbConnection == null) {
            synchronized (DBConnectionPool.class) {
                if (dbConnection == null) {
                    dbConnection = new DBConnectionPool();
                }
            }
        }
        return dbConnection;
    }

    /**
     * 获取数据库连接
     *
     * @return 数据库连接
     */
    public final synchronized Connection getConnection() throws SQLException {
        return cpds.getConnection();
    }

    /**
     * finalize()方法是在垃圾收集器删除对象之前对这个对象调用的。
     *
     * @throws Throwable
     */
    protected void finalize() throws Throwable {
        DataSources.destroy(cpds);
        super.finalize();
    }
}

```

#三.实现新增、修改、删除、查询操作的两个核心方法：可以实现任何复杂的SQL,而且通过数据绑定的方式不会有SQL注入问题

```java
    /**
     * 可以执行新增，修改，删除
     *
     * @param sql      sql语句
     * @param bindArgs 绑定参数
     * @return 影响的行数
     * @throws SQLException SQL异常
     */
    public static int executeUpdate(String sql, Object[] bindArgs) throws SQLException {
        /**影响的行数**/
        int affectRowCount = -1;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            /**从数据库连接池中获取数据库连接**/
            connection = DBConnectionPool.getInstance().getConnection();
            /**执行SQL预编译**/
            preparedStatement = connection.prepareStatement(sql.toString());
            /**设置不自动提交，以便于在出现异常的时候数据库回滚**/
            connection.setAutoCommit(false);
            System.out.println(getExecSQL(sql, bindArgs));
            if (bindArgs != null) {
                /**绑定参数设置sql占位符中的值**/
                for (int i = 0; i < bindArgs.length; i++) {
                    preparedStatement.setObject(i + 1, bindArgs[i]);
                }
            }
            /**执行sql**/
            affectRowCount = preparedStatement.executeUpdate();
            connection.commit();
            String operate;
            if (sql.toUpperCase().indexOf("DELETE FROM") != -1) {
                operate = "删除";
            } else if (sql.toUpperCase().indexOf("INSERT INTO") != -1) {
                operate = "新增";
            } else {
                operate = "修改";
            }
            System.out.println("成功" + operate + "了" + affectRowCount + "行");
            System.out.println();
        } catch (Exception e) {
            if (connection != null) {
                connection.rollback();
            }
            e.printStackTrace();
            throw e;
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
        return affectRowCount;
    }

    
   /**
     * 执行查询
     *
     * @param sql      要执行的sql语句
     * @param bindArgs 绑定的参数
     * @return List<Map<String, Object>>结果集对象
     * @throws SQLException SQL执行异常
     */
    public static List<Map<String, Object>> executeQuery(String sql, Object[] bindArgs) throws SQLException {
        List<Map<String, Object>> datas = new ArrayList<>();
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            /**获取数据库连接池中的连接**/
            connection = DBConnectionPool.getInstance().getConnection();
            preparedStatement = connection.prepareStatement(sql);
            if (bindArgs != null) {
                /**设置sql占位符中的值**/
                for (int i = 0; i < bindArgs.length; i++) {
                    preparedStatement.setObject(i + 1, bindArgs[i]);
                }
            }
            System.out.println(getExecSQL(sql, bindArgs));
            /**执行sql语句，获取结果集**/
            resultSet = preparedStatement.executeQuery();
            getDatas(resultSet);
            System.out.println();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
        return datas;
    }

```

#四.执行新增的简化操作

```java
 /**
     * 执行数据库插入操作
     *
     * @param valueMap  插入数据表中key为列名和value为列对应的值的Map对象
     * @param tableName 要插入的数据库的表名
     * @return 影响的行数
     * @throws SQLException SQL异常
     */
    public static int insert(String tableName, Map<String, Object> valueMap) throws SQLException {

        /**获取数据库插入的Map的键值对的值**/
        Set<String> keySet = valueMap.keySet();
        Iterator<String> iterator = keySet.iterator();
        /**要插入的字段sql，其实就是用key拼起来的**/
        StringBuilder columnSql = new StringBuilder();
        /**要插入的字段值，其实就是？**/
        StringBuilder unknownMarkSql = new StringBuilder();
        Object[] bindArgs = new Object[valueMap.size()];
        int i = 0;
        while (iterator.hasNext()) {
            String key = iterator.next();
            columnSql.append(i == 0 ? "" : ",");
            columnSql.append(key);

            unknownMarkSql.append(i == 0 ? "" : ",");
            unknownMarkSql.append("?");
            bindArgs[i] = valueMap.get(key);
            i++;
        }
        /**开始拼插入的sql语句**/
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ");
        sql.append(tableName);
        sql.append(" (");
        sql.append(columnSql);
        sql.append(" )  VALUES (");
        sql.append(unknownMarkSql);
        sql.append(" )");
        return executeUpdate(sql.toString(), bindArgs);
    }
```

#五.执行更新的简化操作

```java
  /**
     * 执行更新操作
     *
     * @param tableName 表名
     * @param valueMap  要更改的值
     * @param whereMap  条件
     * @return 影响的行数
     * @throws SQLException SQL异常
     */
    public static int update(String tableName, Map<String, Object> valueMap, Map<String, Object> whereMap) throws SQLException {
        /**获取数据库插入的Map的键值对的值**/
        Set<String> keySet = valueMap.keySet();
        Iterator<String> iterator = keySet.iterator();
        /**开始拼插入的sql语句**/
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ");
        sql.append(tableName);
        sql.append(" SET ");

        /**要更改的的字段sql，其实就是用key拼起来的**/
        StringBuilder columnSql = new StringBuilder();
        int i = 0;
        List<Object> objects = new ArrayList<>();
        while (iterator.hasNext()) {
            String key = iterator.next();
            columnSql.append(i == 0 ? "" : ",");
            columnSql.append(key + " = ? ");
            objects.add(valueMap.get(key));
            i++;
        }
        sql.append(columnSql);

        /**更新的条件:要更改的的字段sql，其实就是用key拼起来的**/
        StringBuilder whereSql = new StringBuilder();
        int j = 0;
        if (whereMap != null && whereMap.size() > 0) {
            whereSql.append(" WHERE ");
            iterator = whereMap.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                whereSql.append(j == 0 ? "" : " AND ");
                whereSql.append(key + " = ? ");
                objects.add(whereMap.get(key));
                j++;
            }
            sql.append(whereSql);
        }
        return executeUpdate(sql.toString(), objects.toArray());
    }
```

#六.执行删除的简化操作

```java
  /**
     * 执行删除操作
     *
     * @param tableName 要删除的表名
     * @param whereMap  删除的条件
     * @return 影响的行数
     * @throws SQLException SQL执行异常
     */
    public static int delete(String tableName, Map<String, Object> whereMap) throws SQLException {
        /**准备删除的sql语句**/
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ");
        sql.append(tableName);

        /**更新的条件:要更改的的字段sql，其实就是用key拼起来的**/
        StringBuilder whereSql = new StringBuilder();
        Object[] bindArgs = null;
        if (whereMap != null && whereMap.size() > 0) {
            bindArgs = new Object[whereMap.size()];
            whereSql.append(" WHERE ");
            /**获取数据库插入的Map的键值对的值**/
            Set<String> keySet = whereMap.keySet();
            Iterator<String> iterator = keySet.iterator();
            int i = 0;
            while (iterator.hasNext()) {
                String key = iterator.next();
                whereSql.append(i == 0 ? "" : " AND ");
                whereSql.append(key + " = ? ");
                bindArgs[i] = whereMap.get(key);
                i++;
            }
            sql.append(whereSql);
        }
        return executeUpdate(sql.toString(), bindArgs);
    }

```

#七.查询的4种玩法

 * 1.执行sql通过 Map<String, Object>限定查询条件查询
 
 ```java
   /**
      * 1..执行sql通过 Map<String, Object>限定查询条件查询
      *
      * @param tableName 表名
      * @param whereMap  where条件
      * @return List<Map<String, Object>>
      * @throws SQLException
      */
     public static List<Map<String, Object>> query(String tableName,
                                                   Map<String, Object> whereMap) throws SQLException {
         String whereClause = "";
         Object[] whereArgs = null;
         if (whereMap != null & whereMap.size() > 0) {
             Iterator<String> iterator = whereMap.keySet().iterator();
             whereArgs = new Object[whereMap.size()];
             int i = 0;
             while (iterator.hasNext()) {
                 String key = iterator.next();
                 whereClause += (i == 0 ? "" : " AND ");
                 whereClause += (key + " = ? ");
                 whereArgs[i] = whereMap.get(key);
                 i++;
             }
         }
         return query(tableName, false, null, whereClause, whereArgs, null, null, null, null);
     }
 ```
 
 * 2.执行sql条件参数绑定形式的查询
 
 ```java
     /**
      * 2.执行sql条件参数绑定形式的查询
      *
      * @param tableName   表名
      * @param whereClause where条件的sql
      * @param whereArgs   where条件中占位符中的值
      * @return List<Map<String, Object>>
      * @throws SQLException
      */
     public static List<Map<String, Object>> query(String tableName,
                                                   String whereClause,
                                                   String[] whereArgs) throws SQLException {
         return query(tableName, false, null, whereClause, whereArgs, null, null, null, null);
     }
 ```
 
  * 3.包含所有的查询条件的查询方法
  
  ```java
      /**
       * 执行全部结构的sql查询
       *
       * @param tableName     表名
       * @param distinct      去重
       * @param columns       要查询的列名
       * @param selection     where条件
       * @param selectionArgs where条件中占位符中的值
       * @param groupBy       分组
       * @param having        筛选
       * @param orderBy       排序
       * @param limit         分页
       * @return List<Map<String, Object>>
       * @throws SQLException
       */
      public static List<Map<String, Object>> query(String tableName,
                                                    boolean distinct,
                                                    String[] columns,
                                                    String selection,
                                                    Object[] selectionArgs,
                                                    String groupBy,
                                                    String having,
                                                    String orderBy,
                                                    String limit) throws SQLException {
          String sql = buildQueryString(distinct, tableName, columns, selection, groupBy, having, orderBy, limit);
          return executeQuery(sql, selectionArgs);
        }
  ```
  
   * 4.通过单纯的sql查询数据,慎用，会有sql注入问题，只是为了方便查询，实际开发中不会去使用这个方法
   
   ```java
    * 4.通过sql查询数据,
        * 慎用，会有sql注入问题只是为了方便查询，实际开发中不会去使用这个方法
        *
        * @param sql
        * @return 查询的数据集合
        * @throws SQLException
        */
       public static List<Map<String, Object>> query(String sql) throws SQLException {
           return executeQuery(sql, null);
       }
   ```
 #八.DBUtil的完整代码
 
 ```java
 package util;
 
 import com.sun.istack.internal.Nullable;
 
 import java.sql.*;
 import java.util.*;
 import java.util.regex.Pattern;
 
 /**
  * 数据库JDBC连接工具类
  * Created by yuandl on 2016-12-16.
  */
 public class DBUtil {
 
     /**
      * 执行数据库插入操作
      *
      * @param valueMap  插入数据表中key为列名和value为列对应的值的Map对象
      * @param tableName 要插入的数据库的表名
      * @return 影响的行数
      * @throws SQLException SQL异常
      */
     public static int insert(String tableName, Map<String, Object> valueMap) throws SQLException {
 
         /**获取数据库插入的Map的键值对的值**/
         Set<String> keySet = valueMap.keySet();
         Iterator<String> iterator = keySet.iterator();
         /**要插入的字段sql，其实就是用key拼起来的**/
         StringBuilder columnSql = new StringBuilder();
         /**要插入的字段值，其实就是？**/
         StringBuilder unknownMarkSql = new StringBuilder();
         Object[] bindArgs = new Object[valueMap.size()];
         int i = 0;
         while (iterator.hasNext()) {
             String key = iterator.next();
             columnSql.append(i == 0 ? "" : ",");
             columnSql.append(key);
 
             unknownMarkSql.append(i == 0 ? "" : ",");
             unknownMarkSql.append("?");
             bindArgs[i] = valueMap.get(key);
             i++;
         }
         /**开始拼插入的sql语句**/
         StringBuilder sql = new StringBuilder();
         sql.append("INSERT INTO ");
         sql.append(tableName);
         sql.append(" (");
         sql.append(columnSql);
         sql.append(" )  VALUES (");
         sql.append(unknownMarkSql);
         sql.append(" )");
         return executeUpdate(sql.toString(), bindArgs);
     }
 
     /**
      * 执行更新操作
      *
      * @param tableName 表名
      * @param valueMap  要更改的值
      * @param whereMap  条件
      * @return 影响的行数
      * @throws SQLException SQL异常
      */
     public static int update(String tableName, Map<String, Object> valueMap, Map<String, Object> whereMap) throws SQLException {
         /**获取数据库插入的Map的键值对的值**/
         Set<String> keySet = valueMap.keySet();
         Iterator<String> iterator = keySet.iterator();
         /**开始拼插入的sql语句**/
         StringBuilder sql = new StringBuilder();
         sql.append("UPDATE ");
         sql.append(tableName);
         sql.append(" SET ");
 
         /**要更改的的字段sql，其实就是用key拼起来的**/
         StringBuilder columnSql = new StringBuilder();
         int i = 0;
         List<Object> objects = new ArrayList<>();
         while (iterator.hasNext()) {
             String key = iterator.next();
             columnSql.append(i == 0 ? "" : ",");
             columnSql.append(key + " = ? ");
             objects.add(valueMap.get(key));
             i++;
         }
         sql.append(columnSql);
 
         /**更新的条件:要更改的的字段sql，其实就是用key拼起来的**/
         StringBuilder whereSql = new StringBuilder();
         int j = 0;
         if (whereMap != null && whereMap.size() > 0) {
             whereSql.append(" WHERE ");
             iterator = whereMap.keySet().iterator();
             while (iterator.hasNext()) {
                 String key = iterator.next();
                 whereSql.append(j == 0 ? "" : " AND ");
                 whereSql.append(key + " = ? ");
                 objects.add(whereMap.get(key));
                 j++;
             }
             sql.append(whereSql);
         }
         return executeUpdate(sql.toString(), objects.toArray());
     }
 
     /**
      * 执行删除操作
      *
      * @param tableName 要删除的表名
      * @param whereMap  删除的条件
      * @return 影响的行数
      * @throws SQLException SQL执行异常
      */
     public static int delete(String tableName, Map<String, Object> whereMap) throws SQLException {
         /**准备删除的sql语句**/
         StringBuilder sql = new StringBuilder();
         sql.append("DELETE FROM ");
         sql.append(tableName);
 
         /**更新的条件:要更改的的字段sql，其实就是用key拼起来的**/
         StringBuilder whereSql = new StringBuilder();
         Object[] bindArgs = null;
         if (whereMap != null && whereMap.size() > 0) {
             bindArgs = new Object[whereMap.size()];
             whereSql.append(" WHERE ");
             /**获取数据库插入的Map的键值对的值**/
             Set<String> keySet = whereMap.keySet();
             Iterator<String> iterator = keySet.iterator();
             int i = 0;
             while (iterator.hasNext()) {
                 String key = iterator.next();
                 whereSql.append(i == 0 ? "" : " AND ");
                 whereSql.append(key + " = ? ");
                 bindArgs[i] = whereMap.get(key);
                 i++;
             }
             sql.append(whereSql);
         }
         return executeUpdate(sql.toString(), bindArgs);
     }
 
     /**
      * 可以执行新增，修改，删除
      *
      * @param sql      sql语句
      * @param bindArgs 绑定参数
      * @return 影响的行数
      * @throws SQLException SQL异常
      */
     public static int executeUpdate(String sql, Object[] bindArgs) throws SQLException {
         /**影响的行数**/
         int affectRowCount = -1;
         Connection connection = null;
         PreparedStatement preparedStatement = null;
         try {
             /**从数据库连接池中获取数据库连接**/
             connection = DBConnectionPool.getInstance().getConnection();
             /**执行SQL预编译**/
             preparedStatement = connection.prepareStatement(sql.toString());
             /**设置不自动提交，以便于在出现异常的时候数据库回滚**/
             connection.setAutoCommit(false);
             System.out.println(getExecSQL(sql, bindArgs));
             if (bindArgs != null) {
                 /**绑定参数设置sql占位符中的值**/
                 for (int i = 0; i < bindArgs.length; i++) {
                     preparedStatement.setObject(i + 1, bindArgs[i]);
                 }
             }
             /**执行sql**/
             affectRowCount = preparedStatement.executeUpdate();
             connection.commit();
             String operate;
             if (sql.toUpperCase().indexOf("DELETE FROM") != -1) {
                 operate = "删除";
             } else if (sql.toUpperCase().indexOf("INSERT INTO") != -1) {
                 operate = "新增";
             } else {
                 operate = "修改";
             }
             System.out.println("成功" + operate + "了" + affectRowCount + "行");
             System.out.println();
         } catch (Exception e) {
             if (connection != null) {
                 connection.rollback();
             }
             e.printStackTrace();
             throw e;
         } finally {
             if (preparedStatement != null) {
                 preparedStatement.close();
             }
             if (connection != null) {
                 connection.close();
             }
         }
         return affectRowCount;
     }
 
     /**
      * 通过sql查询数据,
      * 慎用，会有sql注入问题
      *
      * @param sql
      * @return 查询的数据集合
      * @throws SQLException
      */
     public static List<Map<String, Object>> query(String sql) throws SQLException {
         return executeQuery(sql, null);
     }
 
     /**
      * 执行sql通过 Map<String, Object>限定查询条件查询
      *
      * @param tableName 表名
      * @param whereMap  where条件
      * @return List<Map<String, Object>>
      * @throws SQLException
      */
     public static List<Map<String, Object>> query(String tableName,
                                                   Map<String, Object> whereMap) throws SQLException {
         String whereClause = "";
         Object[] whereArgs = null;
         if (whereMap != null & whereMap.size() > 0) {
             Iterator<String> iterator = whereMap.keySet().iterator();
             whereArgs = new Object[whereMap.size()];
             int i = 0;
             while (iterator.hasNext()) {
                 String key = iterator.next();
                 whereClause += (i == 0 ? "" : " AND ");
                 whereClause += (key + " = ? ");
                 whereArgs[i] = whereMap.get(key);
                 i++;
             }
         }
         return query(tableName, false, null, whereClause, whereArgs, null, null, null, null);
     }
 
     /**
      * 执行sql条件参数绑定形式的查询
      *
      * @param tableName   表名
      * @param whereClause where条件的sql
      * @param whereArgs   where条件中占位符中的值
      * @return List<Map<String, Object>>
      * @throws SQLException
      */
     public static List<Map<String, Object>> query(String tableName,
                                                   String whereClause,
                                                   String[] whereArgs) throws SQLException {
         return query(tableName, false, null, whereClause, whereArgs, null, null, null, null);
     }
 
     /**
      * 执行全部结构的sql查询
      *
      * @param tableName     表名
      * @param distinct      去重
      * @param columns       要查询的列名
      * @param selection     where条件
      * @param selectionArgs where条件中占位符中的值
      * @param groupBy       分组
      * @param having        筛选
      * @param orderBy       排序
      * @param limit         分页
      * @return List<Map<String, Object>>
      * @throws SQLException
      */
     public static List<Map<String, Object>> query(String tableName,
                                                   boolean distinct,
                                                   String[] columns,
                                                   String selection,
                                                   Object[] selectionArgs,
                                                   String groupBy,
                                                   String having,
                                                   String orderBy,
                                                   String limit) throws SQLException {
         String sql = buildQueryString(distinct, tableName, columns, selection, groupBy, having, orderBy, limit);
         return executeQuery(sql, selectionArgs);
 
     }
 
     /**
      * 执行查询
      *
      * @param sql      要执行的sql语句
      * @param bindArgs 绑定的参数
      * @return List<Map<String, Object>>结果集对象
      * @throws SQLException SQL执行异常
      */
     public static List<Map<String, Object>> executeQuery(String sql, Object[] bindArgs) throws SQLException {
         List<Map<String, Object>> datas = new ArrayList<>();
         Connection connection = null;
         PreparedStatement preparedStatement = null;
         ResultSet resultSet = null;
 
         try {
             /**获取数据库连接池中的连接**/
             connection = DBConnectionPool.getInstance().getConnection();
             preparedStatement = connection.prepareStatement(sql);
             if (bindArgs != null) {
                 /**设置sql占位符中的值**/
                 for (int i = 0; i < bindArgs.length; i++) {
                     preparedStatement.setObject(i + 1, bindArgs[i]);
                 }
             }
             System.out.println(getExecSQL(sql, bindArgs));
             /**执行sql语句，获取结果集**/
             resultSet = preparedStatement.executeQuery();
             getDatas(resultSet);
             System.out.println();
         } catch (Exception e) {
             e.printStackTrace();
             throw e;
         } finally {
             if (resultSet != null) {
                 resultSet.close();
             }
             if (preparedStatement != null) {
                 preparedStatement.close();
             }
             if (connection != null) {
                 connection.close();
             }
         }
         return datas;
     }
 
 
     /**
      * 将结果集对象封装成List<Map<String, Object>> 对象
      *
      * @param resultSet 结果多想
      * @return 结果的封装
      * @throws SQLException
      */
     private static List<Map<String, Object>> getDatas(ResultSet resultSet) throws SQLException {
         List<Map<String, Object>> datas = new ArrayList<>();
         /**获取结果集的数据结构对象**/
         ResultSetMetaData metaData = resultSet.getMetaData();
         while (resultSet.next()) {
             Map<String, Object> rowMap = new HashMap<>();
             for (int i = 1; i <= metaData.getColumnCount(); i++) {
                 rowMap.put(metaData.getColumnName(i), resultSet.getObject(i));
             }
             datas.add(rowMap);
         }
         System.out.println("成功查询到了" + datas.size() + "行数据");
         for (int i = 0; i < datas.size(); i++) {
             Map<String, Object> map = datas.get(i);
             System.out.println("第" + (i + 1) + "行：" + map);
         }
         return datas;
     }
 
 
     /**
      * Build an SQL query string from the given clauses.
      *
      * @param distinct true if you want each row to be unique, false otherwise.
      * @param tables   The table names to compile the query against.
      * @param columns  A list of which columns to return. Passing null will
      *                 return all columns, which is discouraged to prevent reading
      *                 data from storage that isn't going to be used.
      * @param where    A filter declaring which rows to return, formatted as an SQL
      *                 WHERE clause (excluding the WHERE itself). Passing null will
      *                 return all rows for the given URL.
      * @param groupBy  A filter declaring how to group rows, formatted as an SQL
      *                 GROUP BY clause (excluding the GROUP BY itself). Passing null
      *                 will cause the rows to not be grouped.
      * @param having   A filter declare which row groups to include in the cursor,
      *                 if row grouping is being used, formatted as an SQL HAVING
      *                 clause (excluding the HAVING itself). Passing null will cause
      *                 all row groups to be included, and is required when row
      *                 grouping is not being used.
      * @param orderBy  How to order the rows, formatted as an SQL ORDER BY clause
      *                 (excluding the ORDER BY itself). Passing null will use the
      *                 default sort order, which may be unordered.
      * @param limit    Limits the number of rows returned by the query,
      *                 formatted as LIMIT clause. Passing null denotes no LIMIT clause.
      * @return the SQL query string
      */
     private static String buildQueryString(
             boolean distinct, String tables, String[] columns, String where,
             String groupBy, String having, String orderBy, String limit) {
         if (isEmpty(groupBy) && !isEmpty(having)) {
             throw new IllegalArgumentException(
                     "HAVING clauses are only permitted when using a groupBy clause");
         }
         if (!isEmpty(limit) && !sLimitPattern.matcher(limit).matches()) {
             throw new IllegalArgumentException("invalid LIMIT clauses:" + limit);
         }
 
         StringBuilder query = new StringBuilder(120);
 
         query.append("SELECT ");
         if (distinct) {
             query.append("DISTINCT ");
         }
         if (columns != null && columns.length != 0) {
             appendColumns(query, columns);
         } else {
             query.append(" * ");
         }
         query.append("FROM ");
         query.append(tables);
         appendClause(query, " WHERE ", where);
         appendClause(query, " GROUP BY ", groupBy);
         appendClause(query, " HAVING ", having);
         appendClause(query, " ORDER BY ", orderBy);
         appendClause(query, " LIMIT ", limit);
         return query.toString();
     }
 
     /**
      * Add the names that are non-null in columns to s, separating
      * them with commas.
      */
     private static void appendColumns(StringBuilder s, String[] columns) {
         int n = columns.length;
 
         for (int i = 0; i < n; i++) {
             String column = columns[i];
 
             if (column != null) {
                 if (i > 0) {
                     s.append(", ");
                 }
                 s.append(column);
             }
         }
         s.append(' ');
     }
 
     /**
      * addClause
      *
      * @param s      the add StringBuilder
      * @param name   clauseName
      * @param clause clauseSelection
      */
     private static void appendClause(StringBuilder s, String name, String clause) {
         if (!isEmpty(clause)) {
             s.append(name);
             s.append(clause);
         }
     }
 
     /**
      * Returns true if the string is null or 0-length.
      *
      * @param str the string to be examined
      * @return true if str is null or zero length
      */
     private static boolean isEmpty(@Nullable CharSequence str) {
         if (str == null || str.length() == 0)
             return true;
         else
             return false;
     }
 
     /**
      * the pattern of limit
      */
     private static final Pattern sLimitPattern =
             Pattern.compile("\\s*\\d+\\s*(,\\s*\\d+\\s*)?");
 
     /**
      * After the execution of the complete SQL statement, not necessarily the actual implementation of the SQL statement
      *
      * @param sql      SQL statement
      * @param bindArgs Binding parameters
      * @return Replace? SQL statement executed after the
      */
     private static String getExecSQL(String sql, Object[] bindArgs) {
         StringBuilder sb = new StringBuilder(sql);
         if (bindArgs != null && bindArgs.length > 0) {
             int index = 0;
             for (int i = 0; i < bindArgs.length; i++) {
                 index = sb.indexOf("?", index);
                 sb.replace(index, index + 1, String.valueOf(bindArgs[i]));
             }
         }
         return sb.toString();
     }
 }

 ```
 
 #九.用法

 * 测试代码
 
```java
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
        System.out.println("数据库的原数据");
        testQuery3();
        testInsert();
        System.out.println("执行插入后的数据");
        testQuery3();
        testUpdate();
        System.out.println("执行修改后的数据");
        testQuery3();
        testDelete();
        System.out.println("执行删除后的数据");
        testQuery3();
        System.out.println("带条件的查询1");
        testQuery2();
        System.out.println("带条件的查询2");
        testQuery1();
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
        map.put("hire_date", new java.sql.Date(System.currentTimeMillis()));
        try {
            int count = DBUtil.insert("emp_test", map);
                  } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    /**
     * 测试更新
     */
    private static void testUpdate() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "测试更新");

        Map<String, Object> whereMap = new HashMap<>();
        whereMap.put("emp_id", "1013");
        try {
            int count = DBUtil.update("emp_test", map, whereMap);
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    /**
     * 查询方式一
     */
    public static void testQuery1() {
        Map<String,Object> whereMap=new HashMap<>();
        whereMap.put("salary","10000");
        try {
            DBUtil.query("emp_test",whereMap);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    /**
     * 查询方式二
     */
    public static void testQuery2() {
        String where = "job = ?  AND salary = ? ";
        String[] whereArgs = new String[]{"clerk", "3000"};

        try {
            List<Map<String, Object>> list = DBUtil.query("emp_test", where, whereArgs);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 查询方式三
     */
    public static void testQuery3() {
        try {
            List<Map<String, Object>> list = DBUtil.query("emp_test", false, null, null, null, null, null, null, null);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

```


 * 打印结果

```
数据库的原数据
SELECT  * FROM emp_test
成功查询到了14行数据
第1行：{DEPT_TEST_ID=10, EMP_ID=1001, SALARY=10000, HIRE_DATE=2010-01-12, BONUS=2000, MANAGER=1005, JOB=Manager, NAME=张无忌}
第2行：{DEPT_TEST_ID=10, EMP_ID=1002, SALARY=8000, HIRE_DATE=2011-01-12, BONUS=1000, MANAGER=1001, JOB=Analyst, NAME=刘苍松}
第3行：{DEPT_TEST_ID=10, EMP_ID=1003, SALARY=9000, HIRE_DATE=2010-02-11, BONUS=1000, MANAGER=1001, JOB=Analyst, NAME=李翊}
第4行：{DEPT_TEST_ID=10, EMP_ID=1004, SALARY=5000, HIRE_DATE=2010-02-11, BONUS=null, MANAGER=1001, JOB=Programmer, NAME=郭芙蓉}
第5行：{DEPT_TEST_ID=20, EMP_ID=1005, SALARY=15000, HIRE_DATE=2008-02-15, BONUS=null, MANAGER=null, JOB=President, NAME=张三丰}
第6行：{DEPT_TEST_ID=20, EMP_ID=1006, SALARY=5000, HIRE_DATE=2009-02-01, BONUS=400, MANAGER=1005, JOB=Manager, NAME=燕小六}
第7行：{DEPT_TEST_ID=20, EMP_ID=1007, SALARY=3000, HIRE_DATE=2009-02-01, BONUS=500, MANAGER=1006, JOB=clerk, NAME=陆无双}
第8行：{DEPT_TEST_ID=30, EMP_ID=1008, SALARY=5000, HIRE_DATE=2009-05-01, BONUS=500, MANAGER=1005, JOB=Manager, NAME=黄蓉}
第9行：{DEPT_TEST_ID=30, EMP_ID=1009, SALARY=4000, HIRE_DATE=2009-02-20, BONUS=null, MANAGER=1008, JOB=salesman, NAME=韦小宝}
第10行：{DEPT_TEST_ID=30, EMP_ID=1010, SALARY=4500, HIRE_DATE=2009-05-10, BONUS=500, MANAGER=1008, JOB=salesman, NAME=郭靖}
第11行：{DEPT_TEST_ID=null, EMP_ID=1011, SALARY=null, HIRE_DATE=null, BONUS=null, MANAGER=null, JOB=null, NAME=于泽成}
第12行：{DEPT_TEST_ID=null, EMP_ID=1012, SALARY=null, HIRE_DATE=2011-08-10, BONUS=null, MANAGER=null, JOB=null, NAME=amy}
第13行：{DEPT_TEST_ID=null, EMP_ID=1014, SALARY=8000, HIRE_DATE=null, BONUS=null, MANAGER=null, JOB=null, NAME=张无忌}
第14行：{DEPT_TEST_ID=20, EMP_ID=1015, SALARY=null, HIRE_DATE=null, BONUS=null, MANAGER=null, JOB=null, NAME=刘苍松}

INSERT INTO emp_test (name,hire_date,job,salary,emp_id )  VALUES (JDBCUtil测试,2016-12-17,developer,10000,1013 )
成功新增了1行

执行插入后的数据
SELECT  * FROM emp_test
成功查询到了15行数据
第1行：{DEPT_TEST_ID=10, EMP_ID=1001, SALARY=10000, HIRE_DATE=2010-01-12, BONUS=2000, MANAGER=1005, JOB=Manager, NAME=张无忌}
第2行：{DEPT_TEST_ID=10, EMP_ID=1002, SALARY=8000, HIRE_DATE=2011-01-12, BONUS=1000, MANAGER=1001, JOB=Analyst, NAME=刘苍松}
第3行：{DEPT_TEST_ID=10, EMP_ID=1003, SALARY=9000, HIRE_DATE=2010-02-11, BONUS=1000, MANAGER=1001, JOB=Analyst, NAME=李翊}
第4行：{DEPT_TEST_ID=10, EMP_ID=1004, SALARY=5000, HIRE_DATE=2010-02-11, BONUS=null, MANAGER=1001, JOB=Programmer, NAME=郭芙蓉}
第5行：{DEPT_TEST_ID=20, EMP_ID=1005, SALARY=15000, HIRE_DATE=2008-02-15, BONUS=null, MANAGER=null, JOB=President, NAME=张三丰}
第6行：{DEPT_TEST_ID=20, EMP_ID=1006, SALARY=5000, HIRE_DATE=2009-02-01, BONUS=400, MANAGER=1005, JOB=Manager, NAME=燕小六}
第7行：{DEPT_TEST_ID=20, EMP_ID=1007, SALARY=3000, HIRE_DATE=2009-02-01, BONUS=500, MANAGER=1006, JOB=clerk, NAME=陆无双}
第8行：{DEPT_TEST_ID=30, EMP_ID=1008, SALARY=5000, HIRE_DATE=2009-05-01, BONUS=500, MANAGER=1005, JOB=Manager, NAME=黄蓉}
第9行：{DEPT_TEST_ID=30, EMP_ID=1009, SALARY=4000, HIRE_DATE=2009-02-20, BONUS=null, MANAGER=1008, JOB=salesman, NAME=韦小宝}
第10行：{DEPT_TEST_ID=30, EMP_ID=1010, SALARY=4500, HIRE_DATE=2009-05-10, BONUS=500, MANAGER=1008, JOB=salesman, NAME=郭靖}
第11行：{DEPT_TEST_ID=null, EMP_ID=1011, SALARY=null, HIRE_DATE=null, BONUS=null, MANAGER=null, JOB=null, NAME=于泽成}
第12行：{DEPT_TEST_ID=null, EMP_ID=1012, SALARY=null, HIRE_DATE=2011-08-10, BONUS=null, MANAGER=null, JOB=null, NAME=amy}
第13行：{DEPT_TEST_ID=null, EMP_ID=1014, SALARY=8000, HIRE_DATE=null, BONUS=null, MANAGER=null, JOB=null, NAME=张无忌}
第14行：{DEPT_TEST_ID=20, EMP_ID=1015, SALARY=null, HIRE_DATE=null, BONUS=null, MANAGER=null, JOB=null, NAME=刘苍松}
第15行：{DEPT_TEST_ID=null, EMP_ID=1013, SALARY=10000, HIRE_DATE=2016-12-17, BONUS=null, MANAGER=null, JOB=developer, NAME=JDBCUtil测试}

UPDATE emp_test SET name = 测试更新  WHERE emp_id = 1013 
成功修改了1行

执行修改后的数据
SELECT  * FROM emp_test
成功查询到了15行数据
第1行：{DEPT_TEST_ID=10, EMP_ID=1001, SALARY=10000, HIRE_DATE=2010-01-12, BONUS=2000, MANAGER=1005, JOB=Manager, NAME=张无忌}
第2行：{DEPT_TEST_ID=10, EMP_ID=1002, SALARY=8000, HIRE_DATE=2011-01-12, BONUS=1000, MANAGER=1001, JOB=Analyst, NAME=刘苍松}
第3行：{DEPT_TEST_ID=10, EMP_ID=1003, SALARY=9000, HIRE_DATE=2010-02-11, BONUS=1000, MANAGER=1001, JOB=Analyst, NAME=李翊}
第4行：{DEPT_TEST_ID=10, EMP_ID=1004, SALARY=5000, HIRE_DATE=2010-02-11, BONUS=null, MANAGER=1001, JOB=Programmer, NAME=郭芙蓉}
第5行：{DEPT_TEST_ID=20, EMP_ID=1005, SALARY=15000, HIRE_DATE=2008-02-15, BONUS=null, MANAGER=null, JOB=President, NAME=张三丰}
第6行：{DEPT_TEST_ID=20, EMP_ID=1006, SALARY=5000, HIRE_DATE=2009-02-01, BONUS=400, MANAGER=1005, JOB=Manager, NAME=燕小六}
第7行：{DEPT_TEST_ID=20, EMP_ID=1007, SALARY=3000, HIRE_DATE=2009-02-01, BONUS=500, MANAGER=1006, JOB=clerk, NAME=陆无双}
第8行：{DEPT_TEST_ID=30, EMP_ID=1008, SALARY=5000, HIRE_DATE=2009-05-01, BONUS=500, MANAGER=1005, JOB=Manager, NAME=黄蓉}
第9行：{DEPT_TEST_ID=30, EMP_ID=1009, SALARY=4000, HIRE_DATE=2009-02-20, BONUS=null, MANAGER=1008, JOB=salesman, NAME=韦小宝}
第10行：{DEPT_TEST_ID=30, EMP_ID=1010, SALARY=4500, HIRE_DATE=2009-05-10, BONUS=500, MANAGER=1008, JOB=salesman, NAME=郭靖}
第11行：{DEPT_TEST_ID=null, EMP_ID=1011, SALARY=null, HIRE_DATE=null, BONUS=null, MANAGER=null, JOB=null, NAME=于泽成}
第12行：{DEPT_TEST_ID=null, EMP_ID=1012, SALARY=null, HIRE_DATE=2011-08-10, BONUS=null, MANAGER=null, JOB=null, NAME=amy}
第13行：{DEPT_TEST_ID=null, EMP_ID=1014, SALARY=8000, HIRE_DATE=null, BONUS=null, MANAGER=null, JOB=null, NAME=张无忌}
第14行：{DEPT_TEST_ID=20, EMP_ID=1015, SALARY=null, HIRE_DATE=null, BONUS=null, MANAGER=null, JOB=null, NAME=刘苍松}
第15行：{DEPT_TEST_ID=null, EMP_ID=1013, SALARY=10000, HIRE_DATE=2016-12-17, BONUS=null, MANAGER=null, JOB=developer, NAME=测试更新}

DELETE FROM emp_test WHERE job = developer  AND emp_id = 1013 
成功删除了1行

执行删除后的数据
SELECT  * FROM emp_test
成功查询到了14行数据
第1行：{DEPT_TEST_ID=10, EMP_ID=1001, SALARY=10000, HIRE_DATE=2010-01-12, BONUS=2000, MANAGER=1005, JOB=Manager, NAME=张无忌}
第2行：{DEPT_TEST_ID=10, EMP_ID=1002, SALARY=8000, HIRE_DATE=2011-01-12, BONUS=1000, MANAGER=1001, JOB=Analyst, NAME=刘苍松}
第3行：{DEPT_TEST_ID=10, EMP_ID=1003, SALARY=9000, HIRE_DATE=2010-02-11, BONUS=1000, MANAGER=1001, JOB=Analyst, NAME=李翊}
第4行：{DEPT_TEST_ID=10, EMP_ID=1004, SALARY=5000, HIRE_DATE=2010-02-11, BONUS=null, MANAGER=1001, JOB=Programmer, NAME=郭芙蓉}
第5行：{DEPT_TEST_ID=20, EMP_ID=1005, SALARY=15000, HIRE_DATE=2008-02-15, BONUS=null, MANAGER=null, JOB=President, NAME=张三丰}
第6行：{DEPT_TEST_ID=20, EMP_ID=1006, SALARY=5000, HIRE_DATE=2009-02-01, BONUS=400, MANAGER=1005, JOB=Manager, NAME=燕小六}
第7行：{DEPT_TEST_ID=20, EMP_ID=1007, SALARY=3000, HIRE_DATE=2009-02-01, BONUS=500, MANAGER=1006, JOB=clerk, NAME=陆无双}
第8行：{DEPT_TEST_ID=30, EMP_ID=1008, SALARY=5000, HIRE_DATE=2009-05-01, BONUS=500, MANAGER=1005, JOB=Manager, NAME=黄蓉}
第9行：{DEPT_TEST_ID=30, EMP_ID=1009, SALARY=4000, HIRE_DATE=2009-02-20, BONUS=null, MANAGER=1008, JOB=salesman, NAME=韦小宝}
第10行：{DEPT_TEST_ID=30, EMP_ID=1010, SALARY=4500, HIRE_DATE=2009-05-10, BONUS=500, MANAGER=1008, JOB=salesman, NAME=郭靖}
第11行：{DEPT_TEST_ID=null, EMP_ID=1011, SALARY=null, HIRE_DATE=null, BONUS=null, MANAGER=null, JOB=null, NAME=于泽成}
第12行：{DEPT_TEST_ID=null, EMP_ID=1012, SALARY=null, HIRE_DATE=2011-08-10, BONUS=null, MANAGER=null, JOB=null, NAME=amy}
第13行：{DEPT_TEST_ID=null, EMP_ID=1014, SALARY=8000, HIRE_DATE=null, BONUS=null, MANAGER=null, JOB=null, NAME=张无忌}
第14行：{DEPT_TEST_ID=20, EMP_ID=1015, SALARY=null, HIRE_DATE=null, BONUS=null, MANAGER=null, JOB=null, NAME=刘苍松}

带条件的查询1
SELECT  * FROM emp_test WHERE job = clerk  AND salary = 3000 
成功查询到了1行数据
第1行：{DEPT_TEST_ID=20, EMP_ID=1007, SALARY=3000, HIRE_DATE=2009-02-01, BONUS=500, MANAGER=1006, JOB=clerk, NAME=陆无双}

带条件的查询2
SELECT  * FROM emp_test WHERE salary = 10000 
成功查询到了1行数据
第1行：{DEPT_TEST_ID=10, EMP_ID=1001, SALARY=10000, HIRE_DATE=2010-01-12, BONUS=2000, MANAGER=1005, JOB=Manager, NAME=张无忌}

```

#十.[GitHub](https://github.com/linglongxin24/JDBCUtil)
 
   