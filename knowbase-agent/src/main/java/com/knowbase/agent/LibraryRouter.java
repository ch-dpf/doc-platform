package com.knowbase.agent;

import java.util.List;
import java.util.UUID;

public interface LibraryRouter {

    List<UUID> route(RouteRequest request);
}
