package net.storagenexus.gametest;

/**
 * The Essentials tests run in their own environment, so they run as their own batch, apart from the storage tests:
 * one of them sets off TNT, and the magnet tests pull every item within 16 blocks, which could reach a neighbouring
 * test's drops.
 */
final class EssentialsEnvironment {
    static final String ID = "storagenexus:essentials";

    private EssentialsEnvironment() {}
}
