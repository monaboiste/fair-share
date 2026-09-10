package com.softwarearchetypes.graphs.fixture.influence

class Fixtures {
    public static final PhysicsProcess THERMAL = new PhysicsProcess("thermal")
    public static final PhysicsProcess CONDUCTIVITY = new PhysicsProcess("conductivity")
    public static final PhysicsProcess SPECTROSCOPY = new PhysicsProcess("spectroscopy")

    public static final Laboratory LAB_A = new Laboratory("Lab A")
    public static final Laboratory LAB_B = new Laboratory("Lab B")
    public static final Laboratory LAB_C = new Laboratory("Lab C")

    static InfrastructureInfluence emptyInfrastructure() {
        InfrastructureInfluence.builder().build()
    }
}
