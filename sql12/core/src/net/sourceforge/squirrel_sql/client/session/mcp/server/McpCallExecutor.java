package net.sourceforge.squirrel_sql.client.session.mcp.server;

import net.sourceforge.squirrel_sql.client.session.mcp.server.jsonobjects.McpSimpleString;
import net.sourceforge.squirrel_sql.client.session.mcp.ui.McpServerContext;

public class McpCallExecutor
{
   private boolean _isQueryExecution;

   private McpCallExecutorCallBack _mcpCallExecutorCallBack = null;
   private McpCall _mcpCall = null;

   private McpSimpleString _sql = null;
   private McpServerContext _mcpServerContext = null;

   private Object _callResult;

   public McpCallExecutor(McpCall mcpCall, McpCallExecutorCallBack mcpCallExecutorCallBack)
   {
      _mcpCall = mcpCall;
      _mcpCallExecutorCallBack = mcpCallExecutorCallBack;
   }

   public static McpCallExecutor forExecuteQuery(McpSimpleString sql, McpServerContext mcpServerContext)
   {
      return new McpCallExecutor(sql, mcpServerContext);
   }

   private McpCallExecutor(McpSimpleString sql, McpServerContext mcpServerContext)
   {
      _sql = sql;
      _mcpServerContext = mcpServerContext;
      _isQueryExecution = true;
      _mcpCall = McpCall.executeQuery;
   }

   public void setCallResult(Object callResult)
   {
      if(false == _isQueryExecution)
      {
         throw new IllegalStateException("McpCallExecutor.setCallResult(...) can be called only for execution results.");
      }
      _callResult = callResult;
   }

   public McpSimpleString getSql()
   {
      if(false == _isQueryExecution)
      {
         throw new IllegalStateException("McpCallExecutor.getSql(...) can be called only for execution results.");
      }

      return _sql;
   }

   public McpServerContext getMcpServerContext()
   {
      if(false == _isQueryExecution)
      {
         throw new IllegalStateException("McpCallExecutor.getMcpServerContext(...) can be called only for execution results.");
      }

      return _mcpServerContext;
   }

   public <T> T executeCall()
   {

      if(null == _callResult)
      {
         if(_isQueryExecution)
         {
            throw new IllegalStateException("Query execution result must be delivered by calling McpCallExecutor.setCallResult(...)");
         }

         _callResult = _mcpCallExecutorCallBack.executeCall();
      }

      return (T) _callResult;
   }

   public McpCall getMcpCall()
   {
      return _mcpCall;
   }

   public boolean hasCallResult()
   {
      return null != _callResult;
   }
}
