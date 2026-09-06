from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import pandas as pd
import numpy as np
import joblib


app = FastAPI(
    title="AATMP ML Service",
    version="1.0"
)


# --------------------------------------------------
# LOAD MODELS
# --------------------------------------------------

regression_model = joblib.load(
    "ml/models/final_delay_regression_model.pkl"
)

classification_model = joblib.load(
    "ml/models/final_delay_classification_model.pkl"
)

classification_threshold = joblib.load(
    "ml/models/classification_threshold.pkl"
)


# --------------------------------------------------
# INPUT MODEL
# --------------------------------------------------

class FlightData(BaseModel):

    from_airport: str
    to_airport: str
    airline: str

    distance: float

    passenger_load_factor: float | None = None
    airline_rating: float | None = None
    airport_rating: float | None = None
    market_share: float | None = None

    scheduled_date: str
    scheduled_departure: str
    scheduled_arrival: str

    windspeed: float | None = None
    weather_description: str | None = None
    precipitation: float | None = None
    humidity: float | None = None
    visibility: float | None = None
    pressure: float | None = None
    cloudcover: float | None = None


# --------------------------------------------------
# FEATURE ENGINEERING
# --------------------------------------------------

def create_features(data):

    date = pd.to_datetime(
        data.scheduled_date
    )

    departure = pd.to_datetime(
        data.scheduled_departure,
        format="%H:%M"
    )

    arrival = pd.to_datetime(
        data.scheduled_arrival,
        format="%H:%M"
    )


    departure_hour = departure.hour
    departure_minute = departure.minute

    arrival_hour = arrival.hour
    arrival_minute = arrival.minute

    day = date.day
    month = date.month
    day_of_week = date.dayofweek

    is_weekend = int(
        day_of_week >= 5
    )


    departure_hour_sin = np.sin(
        2 * np.pi * departure_hour / 24
    )

    departure_hour_cos = np.cos(
        2 * np.pi * departure_hour / 24
    )

    day_of_week_sin = np.sin(
        2 * np.pi * day_of_week / 7
    )

    day_of_week_cos = np.cos(
        2 * np.pi * day_of_week / 7
    )


    if departure_hour < 6:
        departure_period = "Night"

    elif departure_hour < 12:
        departure_period = "Morning"

    elif departure_hour < 17:
        departure_period = "Afternoon"

    elif departure_hour < 21:
        departure_period = "Evening"

    else:
        departure_period = "Night"


    row = {
        "From": data.from_airport,
        "To": data.to_airport,
        "Airline": data.airline,
        "Distance": data.distance,

        "Passenger Load Factor":
            data.passenger_load_factor,

        "Airline Rating":
            data.airline_rating,

        "Airport Rating":
            data.airport_rating,

        "Market Share":
            data.market_share,

        "weather__hourly__windspeedKmph":
            data.windspeed,

        "weather__hourly__weatherDesc__value":
            data.weather_description,

        "weather__hourly__precipMM":
            data.precipitation,

        "weather__hourly__humidity":
            data.humidity,

        "weather__hourly__visibility":
            data.visibility,

        "weather__hourly__pressure":
            data.pressure,

        "weather__hourly__cloudcover":
            data.cloudcover,

        "Day": day,
        "Month": month,
        "DayOfWeek": day_of_week,
        "IsWeekend": is_weekend,

        "DepartureHour": departure_hour,
        "DepartureMinute": departure_minute,

        "ArrivalHour": arrival_hour,
        "ArrivalMinute": arrival_minute,

        "DepartureHourSin":
            departure_hour_sin,

        "DepartureHourCos":
            departure_hour_cos,

        "DayOfWeekSin":
            day_of_week_sin,

        "DayOfWeekCos":
            day_of_week_cos,

        "DeparturePeriod":
            departure_period
    }

    return pd.DataFrame([row])


# --------------------------------------------------
# PREDICTION
# --------------------------------------------------

@app.post("/predict")
def predict(data: FlightData):

    try:

        features = create_features(data)


        predicted_delay = float(
            regression_model.predict(features)[0]
        )


        delay_probability = float(
            classification_model.predict_proba(
                features
            )[0][1]
        )


        delay_over_15 = (
            delay_probability >= classification_threshold
        )


        predicted_delay = max(
            0,
            predicted_delay
        )


        if predicted_delay <= 15:
            category = "Low"

        elif predicted_delay <= 45:
            category = "Moderate"

        else:
            category = "High"


        return {
            "predicted_delay_minutes":
                round(predicted_delay, 2),

            "delay_probability":
                round(delay_probability, 4),

            "delay_over_15_minutes":
                bool(delay_over_15),

            "category":
                category
        }


    except Exception as e:

        raise HTTPException(
            status_code=500,
            detail=str(e)
        )


# --------------------------------------------------
# HEALTH CHECK
# --------------------------------------------------

@app.get("/health")
def health():

    return {
        "status": "ok"
    }