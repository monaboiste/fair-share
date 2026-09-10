package com.softwarearchetypes.graphs.fixture.cycles


import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors

interface SlotRepository {
    Optional<Slot> findById(SlotId slotId)

    void save(Slot slot)

    void saveAll(Collection<Slot> values)

    /** Returns detached copies so changes take effect only after they are explicitly saved. */
    Map<SlotId, Slot> findAll(Set<SlotId> allSlotIds)
}

class InMemorySlotRepository implements SlotRepository {

    private final Map<SlotId, Slot> slots = new ConcurrentHashMap<>()

    @Override
    public Optional<Slot> findById(SlotId slotId) {
        return Optional.ofNullable(slots.get(slotId))
    }

    @Override
    public void save(Slot slot) {
        slots.put(slot.id(), slot)
    }

    @Override
    public Map<SlotId, Slot> findAll(Set<SlotId> allSlotIds) {
        return allSlotIds.stream().collect(Collectors.toMap(slotId -> slotId, slotId -> findById(slotId)
                .map(original -> Slot.create(original.id(), original.getOwner()))
                .orElseThrow(NullPointerException::new)))
    }

    @Override
    public void saveAll(Collection<Slot> slots) {
        slots.forEach(this::save)
    }
}
