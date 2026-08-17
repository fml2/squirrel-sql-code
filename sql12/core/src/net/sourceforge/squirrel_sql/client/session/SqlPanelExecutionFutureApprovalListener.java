package net.sourceforge.squirrel_sql.client.session;

import net.sourceforge.squirrel_sql.client.session.mainpanel.CancelPanelCtrl;
import net.sourceforge.squirrel_sql.client.session.mainpanel.ErrorPanel;

public interface SqlPanelExecutionFutureApprovalListener
{
   void displayCancelPanelCtrl(CancelPanelCtrl cancelPanelCtrl);

   void displayErrorPanel(ErrorPanel errorPanel);
}
