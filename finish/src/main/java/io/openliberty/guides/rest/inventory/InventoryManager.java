// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
 // end::copyright[]
package io.openliberty.guides.rest.inventory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

// CDI
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
// JSON-P
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.Timed;

import io.openliberty.guides.rest.util.InventoryUtil;
import io.openliberty.guides.rest.util.JsonMessages;

@ApplicationScoped
public class InventoryManager {

    @Inject
    MetricRegistry registry;

    private ConcurrentMap<String, JsonObject> inv = new ConcurrentHashMap<>();

    @Timed(unit = "nanoseconds", name = "testDataCalcTimer", description="Time needed to get the properties of a hostname")
    public JsonObject get(String hostname) {
        if (InventoryUtil.responseOk(hostname)) {
            JsonObject properties = InventoryUtil.getProperties(hostname);
            inv.putIfAbsent(hostname, properties);
            return properties;
        } else {
            return JsonMessages.SERVICE_UNREACHABLE.getJson();
        }
    }

    @Counted(name = "Number of times the list of hosts is requested", monotonic = true, absolute = true)
    public JsonObject list() {
        JsonObjectBuilder systems = Json.createObjectBuilder();
        inv.forEach((host, props) -> {
            JsonObject systemProps = Json.createObjectBuilder()
                                              .add("os.name", props.getString("os.name"))
                                              .add("user.name", props.getString("user.name"))
                                              .build();
            systems.add(host, systemProps);
        });
        systems.add("hosts", systems);
        systems.add("total", inv.size());
        return systems.build();
    }

    @Gauge (unit="hosts", displayName="Number of hosts", description="Number of hosts in the inventory.")
    public int getHostCount(){
    return inv.size();
    }
}
