package be.coekaerts.wouter.flowtracker.web;

import be.coekaerts.wouter.flowtracker.tracker.ContentTracker;
import be.coekaerts.wouter.flowtracker.tracker.InterestRepository;
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
        item.add(new Label("content", item.getModelObject().getContent().toString()));
      }
    });

  }
}
