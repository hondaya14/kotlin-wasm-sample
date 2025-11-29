package co.hondaya.kotlin_wasm_sample

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform