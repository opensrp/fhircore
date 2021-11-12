package java.beans

class Introspector {
    companion object {
        @JvmStatic
        fun decapitalize(string: String) = string.replaceFirstChar { it.lowercase() }
    }
}