package fairshare.application.group

import fairshare.domain.group.GroupId

public interface GroupStore {
    /** Read the latest committed stream in recorded order, or null if the Group does not exist. */
    public fun load(id: GroupId): GroupStream?

    /**
     * Atomically append every event in [batch] in order under a single expected revision, or append none.
     * Null [expected] requires an absent stream and an opening creation. An existing stream cannot be re-created.
     * A successful append advances the Group to the returned opaque revision.
     */
    public fun append(id: GroupId, expected: GroupRevision?, batch: GroupEventBatch): GroupAppend
}
