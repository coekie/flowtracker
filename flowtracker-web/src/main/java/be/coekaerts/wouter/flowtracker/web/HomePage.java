package be.coekaerts.wouter.flowtracker.web;

import be.coekaerts.wouter.flowtracker.tracker.ContentTracker;
import be.coekaerts.wouter.flowtracker.tracker.InterestRepository;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import java.util.ArrayList;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;

public class HomePage extends WebPage {
  public HomePage() {
    // future tip: render different kinds of trackers with
    // http://wicket.apache.org/learn/examples/usingfragments.html ?

    add(new ListView<ContentTracker>("tracker", new ArrayList<ContentTracker>(InterestRepository.getContentTrackers())) {
      @Override protected void populateItem(ListItem<ContentTracker> item) {
        ContentTracker tracker = item.getModelObject();
        item.add(new Label("description", getRecursiveDescription(tracker)));
      }
    });
  }

  private String getRecursiveDescription(Tracker tracker) {
    return tracker.getDescriptorTracker() == null ? tracker.getDescriptor()
        : tracker.getDescriptor() + " from "
            + getRecursiveDescription(tracker.getDescriptorTracker());
  }
}
