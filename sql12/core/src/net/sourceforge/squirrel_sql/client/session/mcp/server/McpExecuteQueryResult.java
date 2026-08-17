package net.sourceforge.squirrel_sql.client.session.mcp.server;

import net.sourceforge.squirrel_sql.client.session.mainpanel.ResultTab;
import net.sourceforge.squirrel_sql.client.session.mcp.server.jsonobjects.McpResultSet;
import org.apache.commons.lang3.StringUtils;

public record McpExecuteQueryResult(McpResultSet mcpResultSet, ResultTab resultTab)
{
   public static McpExecuteQueryResult ofError(String errorMessage)
   {
      return new McpExecuteQueryResult(McpResultSet.ofError(errorMessage), null);
   }

   public static McpExecuteQueryResult ofUpdateMessage(String updateMessage)
   {
      return new McpExecuteQueryResult(McpResultSet.ofUpdateMessage(updateMessage), null);
   }

   public boolean isUpdateMessage()
   {
      return StringUtils.isNotBlank(mcpResultSet.updateMessage());
   }

   public String getUpdateMessage()
   {
      if( false == isUpdateMessage())
      {
         throw new IllegalStateException("This result is not a mere update message");
      }

      return mcpResultSet.updateMessage();
   }

   public boolean isErrorMessage()
   {
      return StringUtils.isNotBlank(mcpResultSet.errorMessage());
   }

   public String getErrorMessage()
   {
      return mcpResultSet.errorMessage();
   }
}
