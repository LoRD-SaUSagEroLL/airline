package com.patson.model

import com.patson.model.airplane._
import com.patson.data.CycleSource
import com.patson.Util

object Computation {
  //distance vs max speed
  val speedLimits = List((300, 350), (400, 500), (400, 700))  
  def calculateDuration(airplaneModel: Model, distance : Int) = {
    var remainDistance = distance
    var duration = 0;
    for ((distanceBucket, maxSpeed) <- speedLimits if(remainDistance > 0)) {
      val speed = Math.min(maxSpeed, airplaneModel.speed)
      if (distanceBucket >= remainDistance) {
        duration += remainDistance * 60 / speed
      } else {
        duration += distanceBucket * 60 / speed
      }
      remainDistance -= distanceBucket
    }
    
    if (remainDistance > 0) {
      duration += remainDistance * 60 / airplaneModel.speed
    }
    duration
  }

  def calculateMaxFrequency(airplaneModel: Model, distance : Int) : Int = {
    if (airplaneModel.range < distance) {
      0
    } else {
      val duration = calculateDuration(airplaneModel, distance)
      val roundTripTime = (duration + airplaneModel.turnoverTime) * 2
      val availableFlightTimePerWeek = (3.5 * 24 * 60).toInt //assume per week only 3 days are "flyable"
      //println(airplaneModel + " distance " + distance + " freq: " + availableFlightTimePerWeek / roundTripTime + " times")
      availableFlightTimePerWeek / roundTripTime
    }
  }
  
  def calculateAge(fromCycle : Int) = {
    val currentCycle = CycleSource.loadCycle()
    currentCycle - fromCycle 
  }
  
  def calculateAirplaneSellValue(airplane : Airplane) : Int = {
    //80% off
    val value = airplane.value * 0.8
    if (value < 0) 0 else value.toInt
  }
  
  def calculateDistance(fromAirport : Airport, toAirport : Airport) : Int = {
    Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude).toInt
  }
  
  def getFlightType(fromAirport : Airport, toAirport : Airport) = { 
    val distance = Util.calculateDistance(fromAirport.latitude, fromAirport.longitude, toAirport.latitude, toAirport.longitude).toInt
    
    import FlightType._
    if (fromAirport.countryCode == toAirport.countryCode) { //domestic
      if (distance <= 1000) {
        SHORT_HAUL_DOMESTIC
      } else {
        LONG_HAUL_DOMESTIC
      }
    } else if (fromAirport.zone == toAirport.zone) { //international but same continent
      if (distance <= 2000) {
        SHORT_HAUL_INTERNATIONAL
      } else {
        LONG_HAUL_INTERNATIONAL
      }
    } else {
      if (distance <= 4000) {
        SHORT_HAUL_INTERCONTINENTAL
      } else if (distance <= 14000) {
        LONG_HAUL_INTERCONTINENTAL
      } else {
        ULTRA_LONG_HAUL_INTERCONTINENTAL
      }
    }
  }
  
  import FlightCategory._
  def getFlightCategory(fromAirport : Airport, toAirport : Airport) : FlightCategory.Value = {
    if (fromAirport.countryCode == toAirport.countryCode) {
      DOMESTIC
    } else if (fromAirport.zone == toAirport.zone) {
      REGIONAL
    } else {
      INTERCONTINENTAL
    }
  }

  /**
   * Returns a normalized income level, should be greater than 0
   */
  def getIncomeLevel(income : Int) : Int = {
    val incomeLevel = (Math.log(income.toDouble / 500) / Math.log(1.1)).toInt
    if (incomeLevel < 1) {
      1
    } else {
      incomeLevel
    }
  }
  
  def getAirportCredits(links : List[Link]) : Int = {
    var credits = 0
    links.foreach { link =>
      credits += link.from.size + link.to.size
    }
    
    credits
  }
    
  
  def getAirportCredits(from : Airport, to : Airport) : Int = {
    from.size + to.size
  }
  
  def getLinkCreationCost(from : Airport, to : Airport) : Int = {
    
    val baseCost = 100000 + (from.income + to.income)
      
    val minAirportSize = Math.min(from.size, to.size) //encourage links for smaller airport
    
    val airportSizeMultiplier = Math.pow(1.5, minAirportSize) 
    val distance = calculateDistance(from, to)
    val distanceMultiplier = distance.toDouble / 5000
    val internationalMultiplier = if (from.countryCode == to.countryCode) 1 else 3
    
    (baseCost * airportSizeMultiplier * distanceMultiplier * internationalMultiplier).toInt 
  }
}