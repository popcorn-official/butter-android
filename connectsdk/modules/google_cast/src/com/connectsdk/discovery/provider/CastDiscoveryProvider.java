/*
 * CastDiscoveryProvider
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Created by Hyun Kook Khang on 20 Feb 2014
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.connectsdk.discovery.provider;

import android.content.Context;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;
import androidx.mediarouter.media.MediaRouter.RouteInfo;
import android.util.Log;

import com.connectsdk.core.Util;
import com.connectsdk.discovery.DiscoveryFilter;
import com.connectsdk.discovery.DiscoveryProvider;
import com.connectsdk.discovery.DiscoveryProviderListener;
import com.connectsdk.service.CastService;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.config.ServiceDescription;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class CastDiscoveryProvider implements DiscoveryProvider {
    private static final long ROUTE_REMOVE_INTERVAL = 3000;

    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    protected MediaRouter.Callback mMediaRouterCallback;

    private List<String> removedUUID = new CopyOnWriteArrayList<String>();
    protected ConcurrentHashMap<String, ServiceDescription> foundServices;
    protected CopyOnWriteArrayList<DiscoveryProviderListener> serviceListeners;

    private Timer removeRoutesTimer;

    boolean isRunning = false;

    public CastDiscoveryProvider(Context context) {
        mMediaRouter = createMediaRouter(context);
        mMediaRouterCallback = new MediaRouterCallback();

        foundServices = new ConcurrentHashMap<String, ServiceDescription>(8, 0.75f, 2);
        serviceListeners = new CopyOnWriteArrayList<DiscoveryProviderListener>();
    }

    protected MediaRouter createMediaRouter(Context context) {
        return MediaRouter.getInstance(context);
    }

    @Override
    public void start() {
        if (isRunning) 
            return;

        isRunning = true;

        if (mMediaRouteSelector == null) {
            try {
                mMediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(CastMediaControlIntent.categoryForCast(
                        CastService.getApplicationID()))
                .build();
            } catch (IllegalArgumentException e) {
                Log.w(Util.T, "Invalid application ID: " + CastService.getApplicationID());
                for (DiscoveryProviderListener listener : serviceListeners) {
                    listener.onServiceDiscoveryFailed(this, new ServiceCommandError(0,
                            "Invalid application ID: " + CastService.getApplicationID(), null));
                }
                return;
            }
        }

        rescan();
    }

    @Override
    public void stop() {
        isRunning = false;

        if (removeRoutesTimer != null) {
            removeRoutesTimer.cancel();
            removeRoutesTimer = null;
        }

        if (mMediaRouter != null) {
            Util.runOnUI(new Runnable() {

                @Override
                public void run() {
                    mMediaRouter.removeCallback(mMediaRouterCallback);
                }
            });
        }
    }

    @Override
    public void restart() {
        stop();
        start();
    }

    @Override
    public void reset() {
        stop();
        foundServices.clear();
    }

    @Override
    public void rescan() {
        Util.runOnUI(new Runnable() {

            @Override
            public void run() {
                mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                        MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
            }
        });
    }

    @Override
    public void addListener(DiscoveryProviderListener listener) {
        serviceListeners.add(listener);
    }

    @Override
    public void removeListener(DiscoveryProviderListener listener) {
        serviceListeners.remove(listener);
    }

    @Override
    public void addDeviceFilter(DiscoveryFilter filter) {}

    @Override
    public void removeDeviceFilter(DiscoveryFilter filter) {}

    @Override
    public void setFilters(java.util.List<DiscoveryFilter> filters) {}

    @Override
    public boolean isEmpty() {
        return false;
    }

    private class MediaRouterCallback extends MediaRouter.Callback {

        @Override
        public void onRouteAdded(MediaRouter router, RouteInfo route) {
            super.onRouteAdded(router, route);

            CastDevice castDevice = CastDevice.getFromBundle(route.getExtras());
            String uuid = castDevice.getDeviceId();

            removedUUID.remove(uuid);

            ServiceDescription foundService = foundServices.get(uuid);

            boolean isNew = foundService == null;
            boolean listUpdateFlag = false;

            if (isNew) {
                foundService = new ServiceDescription(CastService.ID, uuid,
                        castDevice.getIpAddress().getHostAddress());
                foundService.setFriendlyName(castDevice.getFriendlyName());
                foundService.setModelName(castDevice.getModelName());
                foundService.setModelNumber(castDevice.getDeviceVersion());
                foundService.setModelDescription(route.getDescription());
                foundService.setPort(castDevice.getServicePort());
                foundService.setServiceID(CastService.ID);
                foundService.setDevice(castDevice);

                listUpdateFlag = true;
            }
            else {
                if (!foundService.getFriendlyName().equals(castDevice.getFriendlyName())) {
                    foundService.setFriendlyName(castDevice.getFriendlyName());
                    listUpdateFlag = true;
                }

                foundService.setDevice(castDevice);
            }

            foundService.setLastDetection(new Date().getTime());

            foundServices.put(uuid, foundService);

            if (listUpdateFlag) {
                for (DiscoveryProviderListener listenter: serviceListeners) {
                    listenter.onServiceAdded(CastDiscoveryProvider.this, foundService);
                }
            }
        }

        @Override
        public void onRouteChanged(MediaRouter router, RouteInfo route) {
            super.onRouteChanged(router, route);

            CastDevice castDevice = CastDevice.getFromBundle(route.getExtras());
            String uuid = castDevice.getDeviceId();

            ServiceDescription foundService = foundServices.get(uuid);

            boolean isNew = foundService == null;
            boolean listUpdateFlag = false;

            if (!isNew) {
                foundService.setIpAddress(castDevice.getIpAddress().getHostAddress());
                foundService.setModelName(castDevice.getModelName());
                foundService.setModelNumber(castDevice.getDeviceVersion());
                foundService.setModelDescription(route.getDescription());
                foundService.setPort(castDevice.getServicePort());
                foundService.setDevice(castDevice);

                if (!foundService.getFriendlyName().equals(castDevice.getFriendlyName())) {
                    foundService.setFriendlyName(castDevice.getFriendlyName());
                    listUpdateFlag = true;
                }

                foundService.setLastDetection(new Date().getTime());

                foundServices.put(uuid, foundService);

                if (listUpdateFlag) {
                    for (DiscoveryProviderListener listenter: serviceListeners) {
                        listenter.onServiceAdded(CastDiscoveryProvider.this, foundService);
                    }
                }
            }
        }

        @Override
        public void onRoutePresentationDisplayChanged(MediaRouter router,
                RouteInfo route) {
            Log.d(Util.T, "onRoutePresentationDisplayChanged: [" + route.getName() + "] ["
                    + route.getDescription() + "]");
            super.onRoutePresentationDisplayChanged(router, route);
        }

        @Override
        public void onRouteRemoved(final MediaRouter router, final RouteInfo route) {
            super.onRouteRemoved(router, route);

            CastDevice castDevice = CastDevice.getFromBundle(route.getExtras());
            String uuid = castDevice.getDeviceId();
            removedUUID.add(uuid);

            // Prevent immediate removing. There are some cases when service is removed and added
            // again after a second.
            if (removeRoutesTimer == null) {
                removeRoutesTimer = new Timer();
                removeRoutesTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        removeServices(route);
                    }
                }, ROUTE_REMOVE_INTERVAL);
            }
        }

        @Override
        public void onRouteVolumeChanged(MediaRouter router, RouteInfo route) {
            Log.d(Util.T, "onRouteVolumeChanged: [" + route.getName() + "] ["
                    + route.getDescription() + "]");
            super.onRouteVolumeChanged(router, route);
        }

        private void removeServices(RouteInfo route) {
            for (String uuid : removedUUID) {
                final ServiceDescription service = foundServices.get(uuid);
                if (service != null) {
                    Log.d(Util.T, "Service [" + route.getName() + "] has been removed");
                    Util.runOnUI(new Runnable() {

                        @Override
                        public void run() {
                            for (DiscoveryProviderListener listener : serviceListeners) {
                                listener.onServiceRemoved(CastDiscoveryProvider.this, service);
                            }
                        }
                    });
                    foundServices.remove(uuid);
                }
            }
            removedUUID.clear();
        }
    }
}
