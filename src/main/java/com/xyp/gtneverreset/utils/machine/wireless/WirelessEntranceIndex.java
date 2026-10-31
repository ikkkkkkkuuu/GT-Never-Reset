package com.xyp.gtneverreset.utils.machine.wireless;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.xyp.gtneverreset.utils.machine.wireless.PhysicalClusterTracker.Cluster;
import com.xyp.gtneverreset.utils.machine.wireless.WirelessChannelManager.Entrance;
import com.xyp.gtnrlib.utils.ImmutableCollections;

import appeng.api.networking.IGridNode;

public class WirelessEntranceIndex {

    private final PhysicalClusterTracker<IGridNode, WirelessFrequency> tracker;
    private final Map<Cluster<IGridNode, WirelessFrequency>, List<Entrance>> byCluster = new IdentityHashMap<>();
    private final Map<IGridNode, List<Entrance>> byNode = new IdentityHashMap<>();
    private List<Entrance> entrances;
    private List<Cluster<IGridNode, WirelessFrequency>> clusters;

    WirelessEntranceIndex(PhysicalClusterTracker<IGridNode, WirelessFrequency> tracker) {
        this.tracker = tracker;
    }

    List<Entrance> forCluster(Cluster<IGridNode, WirelessFrequency> cluster) {
        refresh();
        return byCluster.getOrDefault(cluster, ImmutableCollections.listOf());
    }

    List<Entrance> atNode(IGridNode node) {
        refresh();
        return byNode.getOrDefault(node, ImmutableCollections.listOf());
    }

    private void refresh() {
        List<Entrance> currentEntrances = WirelessChannelManager.entrances();
        List<Cluster<IGridNode, WirelessFrequency>> currentClusters = tracker.clusters();
        if (entrances == currentEntrances && clusters == currentClusters) return;
        byCluster.clear();
        byNode.clear();
        for (Entrance entrance : currentEntrances) {
            byNode.computeIfAbsent(entrance.targetNode(), ignored -> new ArrayList<>())
                .add(entrance);
            Cluster<IGridNode, WirelessFrequency> cluster = tracker.clusterOf(entrance.targetNode());
            if (cluster != null) byCluster.computeIfAbsent(cluster, ignored -> new ArrayList<>())
                .add(entrance);
        }
        byCluster.replaceAll((cluster, entries) -> ImmutableCollections.copyList(entries));
        byNode.replaceAll((node, entries) -> ImmutableCollections.copyList(entries));
        entrances = currentEntrances;
        clusters = currentClusters;
    }
}
