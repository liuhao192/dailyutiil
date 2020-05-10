package ren.kura.utils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @ClassName: SqlParserUtil
 * @Descripton: SqlParserUtil sql解析的工具类
 * @author: liuhao
 * @date: 2019/11/6 11:33
 */

public class SqlParserUtil {
      private final static Logger logger = LoggerFactory.getLogger(SqlParserUtil.class);

    private static final Map<String, String> SQL_MAP = new ConcurrentHashMap<String, String>(7);

    private static final AtomicReference<Thread> atomicReference = new AtomicReference<Thread>();

    public enum  SqlParserEnum {
        /**
         * SELECT 类型
         */
        SELECT;

        SqlParserEnum() {

        }
    }

    private enum SqlChainNameEnum {
        SELECT,
        FROM,
        JOIN,
        WHERE,
        GROUP,
        HAVING,
        ORDER
    }

    private interface SqlChain {
        void doFilter(String sql);
    }

    private interface SqlHandler {
        /**
         * 解析当前传入的sql语句，返回处理后的结果
         *
         * @param sql   sql语句
         * @param chain 下一步的处理链
         */
        void handleSql(String sql, SqlChain chain);

        String getChainName();
    }

    private interface SqlHandlerContext {

        String getSql();

        SqlChain getChain();

        String getChainName();

        Pattern getPattern();

    }

    private static class SqlHandlerContextImpl implements SqlHandlerContext {

        private final String sql;
        private final String chainName;
        private final SqlChain chain;
        private final Pattern pattern;

        public SqlHandlerContextImpl(String sql, SqlChainNameEnum chainName, SqlChain chain, Pattern pattern) {
            this.sql = sql;
            this.chainName = chainName.toString();
            this.chain = chain;
            this.pattern = pattern;
        }

        @Override
        public String getSql() {
            return sql;
        }

        @Override
        public SqlChain getChain() {
            return chain;
        }

        @Override
        public String getChainName() {
            return chainName;
        }

        @Override
        public Pattern getPattern() {
            return pattern;
        }
    }

    private static void resolveSqlAndNextFilter(SqlHandlerContext context) {
        int[] sub = resolveSql(context.getSql(), context.getPattern());
        setSqlMap(context.getChainName(), getResolveSql(context.getSql(), sub[0], sub[1]));
        doNextFilter(context.getSql(), context.getChain(), sub[1]);
    }

    private static void setSqlMap(String chainName, String resolveSql) {
        SQL_MAP.put(chainName, resolveSql);
    }

    private static Map<String, String> getSqlMap() {
        Map<String, String> map = new HashMap<>();
        if (!SQL_MAP.isEmpty()) {
            map.putAll(SQL_MAP);
        }
        return map;
    }

    private static void cleanMap() {
        if (!SQL_MAP.isEmpty()) {
            SQL_MAP.clear();
        }
    }

    private static String getResolveSql(String sql, int start, int end) {
        return start == 0 && end == 0 ? "" : sql.substring(start, end);
    }

    private static int[] resolveSql(String sql, Pattern pattern) {
        Matcher matcher = pattern.matcher(sql);
        int start = 0;
        int end = 0;
        if (matcher.find()) {
            start = matcher.start(1);
            end = matcher.end(2);
        }
        return new int[]{start, end};
    }

    private static void doNextFilter(String sql, SqlChain chain, int end) {
        if (chain != null) {
            chain.doFilter(sql.substring(end));
        }
    }

    private static class SelectHandler implements SqlHandler {

        private final Pattern SELECT_PATTERN = Pattern.compile("(SELECT)(.+?)(FROM)");

        @Override
        public void handleSql(String sql, SqlChain chain) {
            resolveSqlAndNextFilter(new SqlHandlerContextImpl(sql, SqlChainNameEnum.SELECT, chain, SELECT_PATTERN));
        }

        @Override
        public String getChainName() {
            return SqlChainNameEnum.SELECT.toString();
        }
    }

    private static class FromHandler implements SqlHandler {
        private final Pattern FROM_PATTERN = Pattern.compile("(FROM)(.+?)(LEFT\\s+JOIN|RIGHT\\s+JOIN|INNER\\s+ JOIN|WHERE|GROUP\\s+BY|HAVING|ORDER\\s+BY| ENDOFSQL)");

        @Override
        public void handleSql(String sql, SqlChain chain) {
            resolveSqlAndNextFilter(new SqlHandlerContextImpl(sql, SqlChainNameEnum.FROM, chain, FROM_PATTERN));
        }
        @Override
        public String getChainName() {
            return SqlChainNameEnum.FROM.toString();
        }
    }

    private static class JoinHandler implements SqlHandler {
        private final Pattern JOIN_PATTERN = Pattern.compile("(LEFT\\s+JOIN|RIGHT\\s+JOIN|INNER\\s+ JOIN)(.+?)(WHERE|HAVING|GROUP\\s+BY|ORDER\\s+BY| ENDOFSQL)");

        /**
         * 解析当前传入的sql语句
         *
         * @param sql   sql语句
         * @param chain 下一步的处理链
         */
        @Override
        public void handleSql(String sql, SqlChain chain) {
            resolveSqlAndNextFilter(
                    new SqlHandlerContextImpl(sql, SqlChainNameEnum.JOIN, chain, JOIN_PATTERN)
            );
        }

        @Override
        public String getChainName() {
            return SqlChainNameEnum.JOIN.toString();
        }
    }

    private static class WhereHandler implements SqlHandler {
        private final Pattern SUB_QUERY_PATTERN = Pattern.compile
                ("(WHERE)(.+?)((\\()(.+?)(\\)))");
        private final Pattern FIRST_WHERE_PATTERN = Pattern.compile
                ("(WHERE)(.+?)");
        private final Pattern FINAL_PATTERN = Pattern.compile
                ("(.+?)(GROUP\\s+BY|HAVING|ORDER\\s+BY)");
        private final Pattern WHERE_PATTERN = Pattern.compile
                ("(WHERE)(.+?)(GROUP\\s+BY|HAVING|ORDER\\s+BY| ENDOFSQL)");

        /**
         * 解析当前传入的sql语句，返回处理后的结果
         *
         * @param sql   sql语句
         * @param chain 下一步的处理链
         */
        @Override
        public void handleSql(String sql, SqlChain chain) {
            Matcher matcher = SUB_QUERY_PATTERN.matcher(sql);
            if (matcher.find()) {
                logger.info("当前的语句含有()的特殊字符");
                //查找第一个where
                Matcher firstWhereMatcher = FIRST_WHERE_PATTERN.matcher(sql);
                int whereEnd = 0;
                if (firstWhereMatcher.find()) {
                    whereEnd = firstWhereMatcher.start(2);
                }
                Matcher finalPattern = FINAL_PATTERN.matcher(sql);
                int finalStart = 0;
                while (finalPattern.find()) {
                    System.out.println(finalPattern.group(2));
                    finalStart = finalPattern.start(2);
                }
                if (whereEnd < finalStart) {
                    setSqlMap(SqlChainNameEnum.WHERE.toString(), getResolveSql(sql, whereEnd, finalStart));
                    chain.doFilter(sql.substring(finalStart));
                }
            } else {
                resolveSqlAndNextFilter(new SqlHandlerContextImpl(sql, SqlChainNameEnum.WHERE, chain, WHERE_PATTERN));
            }
            /**
             * 1.判断是否存在子查询  是否含有()的字符
             * 2.不存在子查询的直接调用之前的处理方法
             * 3.存在() 查询第一个where 的位置 然后在查询
             * GROUP\s+BY|HAVING|ORDER\s+BY| ENDOFSQL 字符所在字符的位置
             */
        }

        @Override
        public String getChainName() {
            return SqlChainNameEnum.WHERE.toString();
        }
    }

    private static class GroupHandler implements SqlHandler {
        private final Pattern GROUP_PATTERN = Pattern.compile
                ("(GROUP\\s+BY)(.+?)(HAVING|ORDER\\s+BY| ENDOFSQL)");

        /**
         * 解析当前传入的sql语句，返回处理后的结果
         *
         * @param sql   sql语句
         * @param chain 下一步的处理链
         */
        @Override
        public void handleSql(String sql, SqlChain chain) {
            resolveSqlAndNextFilter(new SqlHandlerContextImpl(sql, SqlChainNameEnum.GROUP, chain, GROUP_PATTERN));
        }

        @Override
        public String getChainName() {
            return SqlChainNameEnum.GROUP.toString();
        }
    }

    private static class HavingHandler implements SqlHandler {
        private final Pattern HAVING_PATTERN = Pattern.compile
                ("(HAVING\\s+BY)(.+?)(ORDER\\s+BY| ENDOFSQL)");

        /**
         * 解析当前传入的sql语句，返回处理后的结果
         *
         * @param sql   sql语句
         * @param chain 下一步的处理链
         */
        @Override
        public void handleSql(String sql, SqlChain chain) {
            resolveSqlAndNextFilter(new SqlHandlerContextImpl(sql, SqlChainNameEnum.HAVING, chain, HAVING_PATTERN));
        }

        @Override
        public String getChainName() {
            return SqlChainNameEnum.HAVING.toString();
        }
    }

    private static class OrderHandler implements SqlHandler {
        private final Pattern ORDER_PATTERN = Pattern.compile
                ("(ORDER\\s+BY)(.+)( ENDOFSQL)");

        /**
         * 解析当前传入的sql语句，返回处理后的结果
         *
         * @param sql   sql语句
         * @param chain 下一步的处理链
         */
        @Override
        public void handleSql(String sql, SqlChain chain) {
            resolveSqlAndNextFilter(new SqlHandlerContextImpl(sql, SqlChainNameEnum.ORDER, chain, ORDER_PATTERN));
        }

        @Override
        public String getChainName() {
            return SqlChainNameEnum.ORDER.toString();
        }
    }

    private static class ApplicationSqlChain implements SqlChain {
        private SqlHandler[] handles = new SqlHandler[0];
        private int pos = 0;//维持过滤器链中的当前位置
        private int n = 0;//过滤器链中的过滤器数量

        public SqlHandler[] getHandles() {
            return handles;
        }

        @Override
        public void doFilter(String sql) {
            internalDoFilter(sql);
        }

        private void internalDoFilter(String sql) {
            if (pos < n) {
                SqlHandler sqlHandler = handles[pos];
                pos++;
                sqlHandler.handleSql(sql, this);
            }
        }

        public void addFilter(SqlHandler sqlHandler) {
            if (handles.length == n) {
                SqlHandler[] arr2 = java.util.Arrays.copyOf(handles, n + 1);
                handles = arr2;
            }
            handles[n++] = sqlHandler;
        }

    }

    private static String replaceAllCarriage(String str) {
        return str.replaceAll("\n", " ");
    }

    private static String pretreatmentSqlToUpperCase(String sql) {
        StringBuffer stringBuffer = new StringBuffer();
        //处理sql语句
        sql =sql.trim();
        sql = replaceAllCarriage(sql);
        stringBuffer.append(sql.toUpperCase()).append(" ENDOFSQL");
        return stringBuffer.toString();
    }


    public static String getParsedSql(Map<String, String> parsedMap, SqlParserEnum type) {
        StringBuffer result=new StringBuffer();
        switch (type) {
            case SELECT:
                //按照责任链的顺序输出
                ApplicationSqlChain chain= getDefaultsSqlChain();
                for (SqlHandler sqlHandler: chain.getHandles()) {
                    result.append(parsedMap.get(sqlHandler.getChainName())).append(" ");
                }
                break;
        }
        return result.toString();
    }

    public static Map<String, String> getParsedMapSql(String sql) {
        sql = pretreatmentSqlToUpperCase(sql);
        ApplicationSqlChain chain = getDefaultsSqlChain();
        Map<String, String> map = new ConcurrentHashMap<>(7);
        spinlock();
        try {
            chain.doFilter(sql);
            map = getSqlMap();
            cleanMap();
        } catch (Exception e) {
            logger.warn("解析查询语句sql异常" + e);
        } finally {
            //解锁
            unSpinLock();
        }
        return map;
    }

    private static ApplicationSqlChain getDefaultsSqlChain() {
        SqlHandler selectHandler = new SelectHandler();
        SqlHandler fromHandler = new FromHandler();
        SqlHandler joinHandler = new JoinHandler();
        SqlHandler whereHandler = new WhereHandler();
        SqlHandler groupHandler = new GroupHandler();
        SqlHandler havingHandler = new HavingHandler();
        SqlHandler orderHandler = new OrderHandler();
        ApplicationSqlChain chain = new ApplicationSqlChain();
        chain.addFilter(selectHandler);
        chain.addFilter(fromHandler);
        chain.addFilter(joinHandler);
        chain.addFilter(whereHandler);
        chain.addFilter(groupHandler);
        chain.addFilter(havingHandler);
        chain.addFilter(orderHandler);
        return chain;
    }

    private static void spinlock() {
        Thread thread = Thread.currentThread();
        while (!atomicReference.compareAndSet(null, thread)) {
        }
        logger.info("正在调用的线程的为:" + thread.getName());
    }

    private  static void unSpinLock() {
        Thread thread = Thread.currentThread();
        atomicReference.compareAndSet(thread, null);
        logger.info("释放资源的线程为:" + thread.getName());
    }





}


