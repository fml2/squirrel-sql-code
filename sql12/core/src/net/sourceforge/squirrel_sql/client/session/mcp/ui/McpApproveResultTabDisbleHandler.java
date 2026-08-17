package net.sourceforge.squirrel_sql.client.session.mcp.ui;

import javax.swing.JComponent;
import net.sourceforge.squirrel_sql.client.session.mainpanel.TabButton;
import net.sourceforge.squirrel_sql.client.session.mainpanel.resulttabactions.ReRunChooserCtrl;

public class McpApproveResultTabDisbleHandler
{
   private TabButton _createResultFrameButtonTabButton;
   private TabButton _closeTabButton;
   private ReRunChooserCtrl _reRunChooserCtrl;

   public TabButton registerCreateResultFrameButton(TabButton tabButton)
   {
      _createResultFrameButtonTabButton = tabButton;
      return tabButton;
   }

   public TabButton registerCloseButton(TabButton tabButton)
   {
      _closeTabButton = tabButton;
      return tabButton;
   }

   public JComponent registerReRunChooser(ReRunChooserCtrl reRunChooserCtrl)
   {
      _reRunChooserCtrl = reRunChooserCtrl;
      return reRunChooserCtrl.getComponent();
   }

   public void prepareMcpApproveDisplay()
   {
      _createResultFrameButtonTabButton.setEnabled(false);
      _closeTabButton.setEnabled(false);
      _reRunChooserCtrl.setEnabled(false);
   }

   public void unprepareMcpApproveDisplay()
   {
      _createResultFrameButtonTabButton.setEnabled(true);
      _closeTabButton.setEnabled(true);
      _reRunChooserCtrl.setEnabled(true);
   }
}
