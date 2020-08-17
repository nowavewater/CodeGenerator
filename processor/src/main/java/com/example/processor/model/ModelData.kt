package com.example.processor.model

data class ModelData (
    val packageName: String, // 1
    val modelName: String, // 2
    val layoutId: Int, // 3
    val viewHolderBindingData: List<ViewHolderBindingData> // 4
)