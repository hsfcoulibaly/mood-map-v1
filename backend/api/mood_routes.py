from fastapi import APIRouter
import random

router = APIRouter()

# Translated from your GitHub 'comfort.js'
COMFORT_MESSAGES = {
    "sad": ["It's okay to feel sad. You're doing your best.", "Sending you a virtual hug.", "Remember that this feeling is temporary."],
    "stressed": ["Take a deep breath. You've got this.", "Break it down into small steps.", "You are more than your grades or your stress."],
    "anxious": ["Name five things you can see right now — grounding can shrink the spiral.", "This feeling is loud, but it is not permanent.", "One small step is enough for right now."],
    "happy": ["Keep that energy going!", "Share your joy with someone today!", "So glad you're having a good day!"],
    "excited": ["Channel that energy into one thing you will remember tomorrow.", "Good vibes — pay it forward when you can.", "Ride the wave; you earned this moment."],
}

@router.get("/comfort")
def get_comfort(mood: str):
    messages = COMFORT_MESSAGES.get(mood.lower(), ["I'm here for you, no matter how you feel."])
    return {"message": random.choice(messages)}