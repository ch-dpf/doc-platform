package com.knowbase.api.facade;

import com.knowbase.api.command.SearchCommand;
import com.knowbase.api.result.SearchResult;

public interface KnowbaseSearchFacade {

    SearchResult search(SearchCommand command);
}
