/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.logging.internal.impl;

class ThrottleState {

    public final long[] buckets;
    private int currentBucket = 0;
    private long lastBucketTime;

    private long runningTotal = 0;
    private long weightedRunningTotal = 0;

    private final double weightedBuckets = 5;
    private final double weightMultiplier = 2;

    private final int windowIntervals = 20;

    private final double bucketDurationMs;

    private final double throttleMaxMessagesPerWindow;

    private long lastThrottledLogTime = 0; // For THROTTLED message
    private final long throttleMessageIntervalMs = 60000; // “THROTTLED” line every 60s per key
    private long lastAccessTime;

    public ThrottleState(int throttleWindowDuration, double throttleMaxMessagesPerWindow) {
        this.throttleMaxMessagesPerWindow = throttleMaxMessagesPerWindow;
        this.bucketDurationMs = throttleWindowDuration / windowIntervals;
        this.buckets = new long[windowIntervals];
        this.lastBucketTime = System.currentTimeMillis();
        this.lastAccessTime = System.currentTimeMillis();
    }

    public synchronized boolean increment() {
        rotateBuckets();
        buckets[currentBucket]++;
        runningTotal++;
        lastAccessTime = System.currentTimeMillis();

        double weight = getBucketWeight(0);
        weightedRunningTotal += weight;

        return runningTotal > throttleMaxMessagesPerWindow;
    }

    public synchronized boolean canLogThrottledMessage(long now) {
        if (now - lastThrottledLogTime >= throttleMessageIntervalMs) {
            lastThrottledLogTime = now;
            return true;
        }

        return false;
    }

    private void rotateBuckets() {
        long now = System.currentTimeMillis();
        int elapsed = (int) ((now - lastBucketTime) / bucketDurationMs);

        if (elapsed > 0) {
            for (int i = 1; i <= Math.min(elapsed, buckets.length); i++) {
                int idx = (currentBucket + i) % buckets.length;

                double weight = getBucketWeight(i);
                weightedRunningTotal -= buckets[idx] * weight;

                runningTotal -= buckets[idx];
                buckets[idx] = 0;
            }
            currentBucket = (currentBucket + elapsed) % buckets.length;
            lastBucketTime += elapsed * bucketDurationMs;
        }
    }

    public double getBucketWeight(int offset) {
        return (offset < weightedBuckets) ? weightMultiplier : 1.0;
    }

    public synchronized long getRunningTotal() {
        return runningTotal;
    }

    public synchronized long getWeightedRunningTotal() {
        return weightedRunningTotal;
    }

    public synchronized long getLastAccessTime() {
        return lastAccessTime;
    }

}