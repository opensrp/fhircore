package org.smartregister.fhircore.quest.util

/**
 * Utility object containing helper functions for common operations.
 */
object Utils {
    /**
     * Removes the '#' prefix from a resource ID string if present.
     *
     * @param resourceIds The resource ID string that may or may not have a '#' prefix
     * @return The resource ID string without the '#' prefix
     */
    fun removeHashPrefix(resourceIds: String): String {
        return if (resourceIds.startsWith("#")) resourceIds.substring(1) else resourceIds
    }
}