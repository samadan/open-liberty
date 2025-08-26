/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.monitor;

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.kernel.productinfo.ProductInfo;

import io.openliberty.microprofile.telemetry.internal.monitor.internal.MappingTable;
import io.openliberty.microprofile.telemetry.internal.monitor.internal.MonitorMetrics;

@Component(service = MonitorMetricsHandler.class, name = "io.openliberty.microprofile.telemetry.internal.monitor.MonitorMetricsHandler", property = {
		"service.vendor=IBM" }, immediate = true)
public class MonitorMetricsHandler {

	private static final TraceComponent tc = Tr.register(MonitorMetricsHandler.class);

	protected ExecutorService execServ;
	protected MappingTable mappingTable;
	protected Set<MonitorMetrics> mmonitorMetricsSet = new HashSet<MonitorMetrics>();
	protected NotificationListener listener;

	private static final Logger logger = Logger.getLogger(MonitorMetricsHandler.class.getName());

	@Activate
	protected void activate(ComponentContext context) {
		logger.warning("DE_BUG: > MonitorMetricsHandler.activate()");
		this.mappingTable = MappingTable.getInstance();
		register();
		addMBeanListener();
		logger.warning("DE_BUG: < MonitorMetricsHandler.activate()");
	}

	@Reference
	public void setExecutorService(ExecutorService execServ) {
		this.execServ = execServ;
	}

	public void unsetExecutorService(ExecutorService execServ) {
		this.execServ = null;
	}

	@Deactivate
	protected void deactivate(ComponentContext context) {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		if (listener != null) {
			try {
				mbs.removeNotificationListener(MBeanServerDelegate.DELEGATE_NAME, listener);
			} catch (Exception e) {
				if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
					Tr.debug(tc, "deactivate exception message: ", e.getMessage());
				}
				FFDCFilter.processException(e, getClass().getSimpleName(), "deactivate:Exception");
			}
			listener = null;
		}
	}

	protected void addMBeanListener() {
		listener = new NotificationListener() {

			@Override
			public void handleNotification(Notification notification, Object handback) {
				MBeanServerNotification mbsn = (MBeanServerNotification) notification;
				String objectName = mbsn.getMBeanName().toString();
				logger.warning("DE_BUG: > MonitorMetricsHandler.handleNotification() for: " + objectName);
				if (MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(mbsn.getType())) {
					if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
						Tr.debug(tc, "MBean Registered [", objectName + "]");
					}
					logger.warning("DE_BUG: = handleNotification() registered: " + objectName);
					String[][] data = mappingTable.getData(objectName);
					if (data != null) {
						register(objectName, data);
					}
				} else if (MBeanServerNotification.UNREGISTRATION_NOTIFICATION.equals(mbsn.getType())) {
					if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
						Tr.debug(tc, "MBean Unregistered [" + objectName + "]");
					}
					logger.warning("DE_BUG: = MonitorMetricsHandler.handleNotification() unregistered: " + objectName);
					if (mappingTable.contains(objectName)) {
						unregister(objectName);
					}
				}
				logger.warning("DE_BUG: < MonitorMetricsHandler.handleNotification() for: " + objectName);
			}
		};

		logger.warning("DE_BUG: > MonitorMetricsHandler.addMBeanListener()");
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			mbs.addNotificationListener(MBeanServerDelegate.DELEGATE_NAME, listener, null, null);
		} catch (InstanceNotFoundException e) {
			if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
				Tr.debug(tc, "Exception while adding listener: ", e.getMessage());
			}
			FFDCFilter.processException(e, getClass().getSimpleName(), "addMBeanListener:Exception");
		}
		logger.warning("DE_BUG: < MonitorMetricsHandler.addMBeanListener()");
	}

	protected void unregister(String objectName) {
		Set<MonitorMetrics> removeSet = new HashSet<MonitorMetrics>();
		for (MonitorMetrics mm : mmonitorMetricsSet) {
			if (mm.getObjectName().equals(objectName)) {
				// TODO : remove
				removeSet.add(mm);
				mm.unregisterMetrics();
				if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
					Tr.debug(tc, "Monitoring MXBean " + objectName + " was unregistered.");
				}

			}
		}
		mmonitorMetricsSet.removeAll(removeSet);
	}

	protected void register() {
		logger.warning("DE_BUG: > MonitorMetricsHandler.register()");
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		logger.warning("DE_BUG: = MonitorMetricsHandler.register(): All mappingTable keys: ");
		for (String sName : mappingTable.getKeys()) {
			logger.warning("DE_BUG: = MonitorMetricsHandler.register(): key: " + sName);
		}
		for (String sName : mappingTable.getKeys()) {
			logger.warning("DE_BUG: = MonitorMetricsHandler.register(): Looking for key: " + sName);
			Set<ObjectInstance> mBeanObjectInstanceSet;
			try {
				mBeanObjectInstanceSet = mbs.queryMBeans(new ObjectName(sName), null);
				logger.warning("DE_BUG: = MonitorMetricsHandler.register() try: All items in mBeanObjectInstanceSet from querying: " + sName);
				for(ObjectInstance oi : mBeanObjectInstanceSet) {
					logger.warning("DE_BUG: = MonitorMetricsHandler.register() try: item: " + oi.getObjectName());
				}
				if (sName.contains("ThreadPoolStats") && mBeanObjectInstanceSet.isEmpty() && execServ != null) {
					logger.warning("DE_BUG: = MonitorMetricsHandler.register() try: trying to get " + sName);
					execServ.execute(() -> {
						final int MAX_TIME_OUT = 5000;
						int currentTimeOut = 0;
						Set<ObjectInstance> mBeanObjectInstanceSetTemp = mBeanObjectInstanceSet;
						while (mBeanObjectInstanceSetTemp.isEmpty() && currentTimeOut <= MAX_TIME_OUT) {
							try {
								logger.warning("DE_BUG: = MonitorMetricsHandler.register() try: mBeanObjectInstanceSet is empty (can't find " + sName + "), trying again...");
								Thread.sleep(50);

								mBeanObjectInstanceSetTemp = mbs.queryMBeans(new ObjectName(sName), null);
								currentTimeOut += 50;
							} catch (Exception e) {
								if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
									Tr.debug(tc, "register exception message: ", e.getMessage());
								}
								FFDCFilter.processException(e, MonitorMetricsHandler.class.getSimpleName(),
										"register:Exception");
								/*
								 * Interruption Exception or RuntimeOperationException from malformed query exit
								 * thread.
								 */
								break;
							}
						}
						if(mBeanObjectInstanceSetTemp.isEmpty()) {
							logger.warning("DE_BUG: = MonitorMetricsHandler.register(): Couldn't find " + sName + " after retries.");
						} else {
							logger.warning("DE_BUG: = MonitorMetricsHandler.register(): Found " + sName + " after retries.");
						}
						registerMbeanObjects(mBeanObjectInstanceSetTemp);
					});
				}
				registerMbeanObjects(mBeanObjectInstanceSet);
			} catch (Exception e) {
				if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
					Tr.debug(tc, "register exception message: ", e.getMessage());

				}
				FFDCFilter.processException(e, MonitorMetricsHandler.class.getSimpleName(), "register:Exception");
			}
		}
		logger.warning("DE_BUG: < MonitorMetricsHandler.register()");
	}

	private synchronized void registerMbeanObjects(Set<ObjectInstance> mBeanObjectInstanceSet) {
		logger.warning("DE_BUG: > MonitorMetricsHandler.registerMbeanObjects()");
		for (ObjectInstance objInstance : mBeanObjectInstanceSet) {
			String objectName = objInstance.getObjectName().toString();
			String[][] data = mappingTable.getData(objectName);
			if (data != null) {
				register(objectName, data);
			}
		}
		logger.warning("DE_BUG: < MonitorMetricsHandler.registerMbeanObjects()");
	}

	protected synchronized void register(String objectName, String[][] data) {
		logger.warning("DE_BUG: > MonitorMetricsHandler.register(objectName, data)");
		MonitorMetrics monitorMetricsInsts = null;
		if (!containMetrics(objectName)) {
			monitorMetricsInsts = new MonitorMetrics(objectName);
			monitorMetricsInsts.createMetrics(data);
			mmonitorMetricsSet.add(monitorMetricsInsts);
			logger.warning("DE_BUG: = MonitorMetricsHandler.register(objectName, data): ADDED MXBEAN " + objectName);
			if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
				Tr.debug(tc, "Monitoring MXBean " + objectName + " is registered to mpTelemetry.");
			}

		} else {
			if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
				Tr.debug(tc, objectName + " is already registered.");
			}
		}
		allMetrics();
		logger.warning("DE_BUG: < MonitorMetricsHandler.register(objectName, data)");
	}

	protected boolean containMetrics(String objectName) {
		for (MonitorMetrics mm : mmonitorMetricsSet) {
			if (mm.getObjectName().equals(objectName))
				return true;
		}
		return false;
	}
	
	protected void allMetrics() {
		logger.warning("DE_BUG: > MonitorMetricsHandler.allMetrics()");
		for (MonitorMetrics mm : mmonitorMetricsSet) {
			logger.warning("DE_BUG: = MonitorMetricsHandler.allMetrics(): " + mm.getObjectName());
		}
		logger.warning("DE_BUG: < MonitorMetricsHandler.allMetrics()");
	}

}
