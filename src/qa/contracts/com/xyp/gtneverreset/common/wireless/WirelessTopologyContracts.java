package com.xyp.gtneverreset.common.wireless;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public final class WirelessTopologyContracts {

    private static final class Node {
        final List<Node> neighbours = new ArrayList<>();
        String saved;
        boolean loaded = true;

        void connect(Node other) {
            neighbours.add(other);
            other.neighbours.add(this);
        }

        void disconnect(Node other) {
            neighbours.remove(other);
            other.neighbours.remove(this);
        }
    }

    private static class Network implements PhysicalClusterTracker.Topology<Node> {
        int visits;

        @Override
        public boolean isLive(Node node) {
            return node.loaded;
        }

        @Override
        public Iterable<Node> neighbours(Node node) {
            visits++;
            return node.neighbours;
        }

        @Override
        public boolean blocksWireless(Node node) {
            return false;
        }
    }

    public static void main(String[] args) {
        var schedule = new WirelessClusterManager.ScanSchedule();
        require(schedule.tick(), "initial scan");
        schedule.beginScan();
        for (int i = 1; i < 10; i++) {
            schedule.invalidate();
            require(!schedule.tick() && schedule.pending(), "pending event must respect cooldown");
        }
        require(schedule.tick(), "pending event must run on tenth tick");

        var network = new Network();
        var local = new PhysicalClusterTracker<Node, String>(32768);
        var full = new PhysicalClusterTracker<Node, String>(32768);
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < 4000; i++) {
            Node first = new Node();
            first.saved = "frequency" + i;
            Node previous = first;
            nodes.add(first);
            for (int j = 1; j < 4; j++) {
                Node next = new Node();
                previous.connect(next);
                nodes.add(next);
                previous = next;
            }
        }
        Function<Node, Set<String>> claims = node -> node.saved == null ? Set.of() : Set.of(node.saved);
        local.refresh(network, nodes, claims, value -> value);
        full.refresh(network, nodes, claims, value -> value);
        var untouched = local.clusterOf(nodes.get(8));
        network.visits = 0;
        local.refreshAround(network, List.of(nodes.get(0)), claims, value -> value);
        require(network.visits == 4, "local scan traversed another subnet");
        require(untouched == local.clusterOf(nodes.get(8)), "unrelated cluster replaced");

        Node end = nodes.get(3), start = nodes.get(4);
        end.connect(start);
        local.refreshAround(network, List.of(start), claims, value -> value);
        full.refresh(network, nodes, claims, value -> value);
        compare(nodes, local, full);
        require(local.clusterOf(start).conflicted(), "merged claims lost");
        end.disconnect(start);
        end.loaded = false;
        local.refreshAround(network, List.of(start), claims, value -> value);
        full.refresh(network, nodes, claims, value -> value);
        compare(nodes, local, full);
        var cluster = local.clusterOf(start);
        local.unlink(cluster, start.saved, value -> value);
        local.commitClaims(cluster, value -> value);
        require(local.claimsOf(start).isEmpty(), "unlinked claim restored");
        require(untouched == local.clusterOf(nodes.get(8)), "claim commit replaced unrelated cluster");
        local.link(local.clusterOf(start), "replacement", value -> value);
        local.commitClaims(local.clusterOf(start), value -> value);
        require(local.claimsOf(nodes.get(7)).equals(Set.of("replacement")), "new claim failed to propagate");

        var limited = new PhysicalClusterTracker<Node, String>(2);
        limited.refreshAround(network, List.of(start), claims, value -> value);
        require(limited.clusterOf(start).limited(), "local scan lost the per-subnet limit");
        System.out.println("PASS: 4000 subnets, 16000 nodes, four visits for one subnet; merge, split, unload, unlink and scan cooldown");
    }

    private static void compare(List<Node> nodes, PhysicalClusterTracker<Node, String> local,
        PhysicalClusterTracker<Node, String> full) {
        for (Node node : nodes) {
            require(Objects.equals(local.clusterOf(node), full.clusterOf(node)), "cluster mismatch");
            require(local.claimsOf(node).equals(full.claimsOf(node)), "claim mismatch");
        }
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
