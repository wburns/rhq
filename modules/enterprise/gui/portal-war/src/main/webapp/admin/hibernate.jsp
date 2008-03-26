<%@ page import="org.rhq.core.domain.util.PersistenceUtility" %>
<%@ page import="javax.persistence.EntityManagerFactory" %>
<%@ page import="javax.naming.InitialContext" %>
<%@ page import="javax.persistence.EntityManager" %>
<%@ page import="org.hibernate.hql.QueryTranslator" %>
<%@ page import="org.hibernate.hql.QueryTranslatorFactory" %>
<%@ page import="org.hibernate.impl.SessionFactoryImpl" %>
<%@ page import="org.hibernate.hql.ast.ASTQueryTranslatorFactory" %>
<%@ page import="org.hibernate.engine.SessionFactoryImplementor" %>
<%@ page import="org.hibernate.engine.SessionImplementor" %>
<%@ page import="org.hibernate.engine.QueryParameters" %>
<%@ page import="java.io.StringBufferInputStream" %>
<%@ page import="java.io.PrintWriter" %>
<%@ page import="java.io.StringWriter" %>
<%@ page import="javax.persistence.Query" %>
<%@ page import="org.hibernate.type.Type" %>
<%@ page import="org.hibernate.type.LongType" %>
<%@ page import="org.hibernate.type.IntegerType" %>
<%@ page import="java.util.*" %>
<%@ page import="org.rhq.enterprise.gui.legacy.util.SessionUtils" %>
<%@ page import="org.rhq.enterprise.server.util.LookupUtil" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%--
  Author: Greg Hinkle
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head><title>JPQL Tranlation and Execution Tool</title></head>
<body>


<form action="/admin/hibernate.jsp" method="post">


    <b>JPQL: </b><textarea name="hql" type="text" rows="8" cols="120"><%=request.getParameter("hql")%></textarea>
    <br/>
    <input name="translate" type="submit" value="translate"/>
    <input name="execute" type="submit" value="execute"/>

<br/>
<%
    if (!LookupUtil.getAuthorizationManager().isSystemSuperuser(SessionUtils.getWebUser(session).getSubject())) // no one but rhqadmin can view this page
    {
       throw new IllegalAccessException("You do not have admin permissions");
    }

    String hql = request.getParameter("hql");
    List results = null;
    long executionTime = 0;
    String error = null;
    QueryTranslator qt = null;
    if (hql != null) {

        InitialContext ic = new InitialContext();

        EntityManager em = ((EntityManagerFactory) ic.lookup("java:/RHQEntityManagerFactory"))
                .createEntityManager();

        org.hibernate.Session s = PersistenceUtility.getHibernateSession(em);


        qt = new ASTQueryTranslatorFactory().createQueryTranslator(
                        "test query",
                        hql,
                        null,
                        (SessionFactoryImplementor) s.getSessionFactory());

        qt.compile(null,true);
        String sql = qt.getSQLString();

        out.write("<b>SQL: </b><textarea rows=\"10\" cols=\"120\">" + sql + "</textarea>");

        Set<String> parameterNames = qt.getParameterTranslations().getNamedParameterNames();
        request.setAttribute("parameterNames", parameterNames);
        %>
<br/>
    <c:if test="${parameterNames != null}">
        <table>
        <c:forEach var="pn" items="${parameterNames}">
            <tr>
            <td><b>${pn}</b></td>
            <td><input type="text" name="${pn}" value="${param[pn]}"></td>
            <td>
            <c:set value="${pn}" var="pn" scope="request"/>
            <%=qt.getParameterTranslations().getNamedParameterExpectedType((String) request.getAttribute("pn")).getName()%>
            </td>
            </tr>
        </c:forEach>
        </table>
    </c:if>
        <%


        if (request.getParameter("execute") != null) {

            long start = System.currentTimeMillis();
            try {
                //results = qt.list((SessionImplementor) s, new QueryParameters());
                Query q = em.createQuery(hql);
                Iterator iter = parameterNames.iterator();
                while(iter.hasNext()) {
                    String pn = (String) iter.next();
                    Object paramterValue = request.getParameter(pn);
                    Type type = qt.getParameterTranslations().getNamedParameterExpectedType(pn);
                    if (type instanceof LongType)
                        paramterValue = Long.parseLong((String) paramterValue);
                    else if (type instanceof IntegerType)
                        paramterValue = Integer.parseInt((String) paramterValue);

                    out.println("parameter " + pn + " = " + paramterValue);
                    q.setParameter(pn,paramterValue);
                }
                results = q.getResultList();
                request.setAttribute("results",results);

            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                error = sw.toString();
                request.setAttribute("error",error);
            }
            executionTime = (System.currentTimeMillis() - start);
        }
    }

%>

</form>


<br/>

<c:if test="${param['execute'] != null and results != null}">
    <b>Executed in <%=executionTime%>ms. Found <%=results.size()%> rows.</b>
</c:if>
<c:if test="${error != null}">
    <pre>${error}</pre>
</c:if>
<c:if test="${results != null}">
    <table border="1">

        <%
            String[] aliases = qt.getReturnAliases();
            out.println("<tr>");
            for (String alias : aliases) {
                out.println("<th>" + alias + "</th>");
            }
            out.println("</tr>");

            for (Object row : results) {
                out.println("<tr>");
                if (row instanceof Object[]) {
                    Object[] arr = (Object[]) row;
                    for (Object col : arr) {
                        out.println("<td>" + col + "</td>");
                    }
                } else {
                    out.println("<td>" + row + "</td>");                    
                }
                out.println("</tr>");
            }
        %>

    </table>
</c:if>



</body>
</html>