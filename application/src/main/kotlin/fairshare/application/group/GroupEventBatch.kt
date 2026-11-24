package fairshare.application.group

import fairshare.domain.group.GroupCreated
import fairshare.domain.group.GroupEvent

public class GroupEventBatch(events: List<GroupEvent>) {
    private val recordedEvents = events.toList()

    public val events: List<GroupEvent> get() = recordedEvents.toList()

    init {
        require(recordedEvents.isNotEmpty()) { "Group event batch must not be empty" }
        recordedEvents.drop(1).forEach { event ->
            require(event !is GroupCreated) { "Group creation can only open a stream" }
        }
    }
}
