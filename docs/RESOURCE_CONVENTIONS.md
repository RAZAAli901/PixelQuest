# Resource conventions

Q016 found no safe duplicate-resource merge. The matching `pq_icon_bg` and `pq_red` values are
deliberately separate semantic tokens: the first is the launcher-icon background and the second is
the notification accent. Keeping both lets either visual role change independently without a hidden
coupling.

New resources should be named by role, not their current literal value. Shared design values belong
in the Compose design-token layer; Android XML resources remain only for platform surfaces such as
notifications and launcher icons.
