/**
 * Copyright (c) 2019-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.mykitaheatpump.internal;

/**
 * The {@link MyKitaHeatPumpConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Marco Tombesi - Initial contribution
 */

public class MyKitaHeatPumpConfiguration {

    public boolean enableDiscovery;

    public String host;
    public int port;
    public int id;

    public int timeBetweenTransactionsMillis;

    public int connectMaxTries;
    public int reconnectAfterMillis;
    public int timeBetweenReconnectMillis;
    public int connectTimeoutMillis;

    // poller
    public long refresh;
    public int maxTries = 3;
    public long cacheMillis = 50L;
}
