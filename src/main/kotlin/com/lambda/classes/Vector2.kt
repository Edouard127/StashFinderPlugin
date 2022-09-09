package com.lambda.classes

import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

class Vector2(var x: Double, var y: Double) {
    fun add(other: Vector2): Vector2 {
        return Vector2(this.x + other.x, this.y + other.y)
    }
    fun sub(other: Vector2): Vector2 {
        return Vector2(this.x - other.x, this.y - other.y)
    }
    fun mul(other: Double): Vector2 {
        return Vector2(this.x * other, this.y * other)
    }
    fun div(other: Double): Vector2 {
        return Vector2(this.x / other, this.y / other)
    }
    fun floor(): Vector2 {
        return Vector2(kotlin.math.floor(this.x), kotlin.math.floor(this.y))
    }
    fun length(): Double {
        return kotlin.math.sqrt((this.x * this.x + this.y * this.y))
    }
    fun normalize(): Vector2 {
        val length = this.length()
        return Vector2(this.x / length, this.y / length)
    }
    fun distance(other: Vector2): Double {
        return this.sub(other).length()
    }
    fun toVec3d(): Vec3d {
        return Vec3d(this.x, 0.0, this.y)
    }
    fun toBlockPos(): BlockPos {
        return BlockPos(this.x.toInt(), 0, this.y.toInt())
    }

    override fun toString(): String {
        return "Vector2(x=$x, y=$y)"
    }
}