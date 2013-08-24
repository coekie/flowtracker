package be.coekaerts.wouter.flowtracker.web;

import org.apache.wicket.Page;
import org.apache.wicket.protocol.http.WebApplication;

public class FlowtrackerApplication extends WebApplication {
  @Override public Class<? extends Page> getHomePage() {
    return HomePage.class;
  }
}
