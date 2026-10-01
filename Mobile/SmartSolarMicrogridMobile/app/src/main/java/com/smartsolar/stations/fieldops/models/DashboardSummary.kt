package com.smartsolar.stations.models

/**
 * Summary metrics for Prosumer dashboard.
 */
data class ProsumerMetrics(
    val activeBookings: Int = 1,
    val pendingBookings: Int = 1,
    val completedBookings: Int = 5
)

/**
 * Summary metrics for Grid Operator dashboard.
 */
data class OperatorMetrics(
    val todayTransactions: Int = 8,
    val pendingVerification: Int = 2,
    val completedTransfers: Int = 6
)
