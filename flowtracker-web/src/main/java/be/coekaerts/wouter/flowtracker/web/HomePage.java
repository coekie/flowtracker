package be.coekaerts.wouter.flowtracker.web;

import be.coekaerts.wouter.flowtracker.tracker.ContentTracker;
import be.coekaerts.wouter.flowtracker.tracker.InterestRepository;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.util.ShutdownSuspender;
import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

public class HomePage extends WebPage {
  public HomePage() {
    // future tip: render different kinds of trackers with
    // http://wicket.apache.org/learn/examples/usingfragments.html ?

    add(new ListView<ContentTracker>("tracker", new TrackersModel()) {
      @Override protected void populateItem(ListItem<ContentTracker> item) {
        ContentTracker tracker = item.getModelObject();
        item.add(new Label("description", getRecursiveDescription(tracker)));
      }
    });

    add(new CheckBox("suspendShutdown", new SuspendShutdownModel()) {
      @Override protected boolean wantOnSelectionChangedNotifications() {
        return true;
      }
    });
  }

  private String getRecursiveDescription(Tracker tracker) {
    return tracker.getDescriptorTracker() == null ? tracker.getDescriptor()
        : tracker.getDescriptor() + " from "
            + getRecursiveDescription(tracker.getDescriptorTracker());
  }

  private static class TrackersModel implements IModel<List<ContentTracker>> {
    @Override public List<ContentTracker> getObject() {
      // TODO guarantee consistent ordering or override getListItemModel in ListView
      // or use RepeatingView / RefreshingView?
      return new ArrayList<ContentTracker>(InterestRepository.getContentTrackers());
    }

    @Override public void setObject(List<ContentTracker> object) {
      throw new UnsupportedOperationException();
    }

    @Override public void detach() {
    }
  }

  private static class SuspendShutdownModel implements IModel<Boolean> {
    @Override public Boolean getObject() {
      return ShutdownSuspender.isSuspendShutdown();
    }

    @Override public void setObject(Boolean b) {
      ShutdownSuspender.setSuspendShutdown(b);

    }

    @Override public void detach() {
    }
  }
}
