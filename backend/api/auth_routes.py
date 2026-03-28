import mysql.connector
from fastapi import APIRouter, HTTPException
from google.auth.transport import requests as google_requests
from google.oauth2 import id_token as google_id_token
from pydantic import BaseModel, ConfigDict, EmailStr, Field

from core.config import get_settings
from core.security import create_access_token, hash_password, verify_password
from database.db_connection import get_db_connection

router = APIRouter()


class LoginRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    email: str
    password: str = Field(alias="pass")


class RegisterRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=10, max_length=128)


class LoginResponse(BaseModel):
    token: str
    message: str


class GoogleTokenRequest(BaseModel):
    id_token: str


@router.post("/google", response_model=LoginResponse)
def google_sign_in(request: GoogleTokenRequest):
    settings = get_settings()
    if not settings.google_web_client_id:
        raise HTTPException(
            status_code=503,
            detail="Google Sign-In is not configured (set GOOGLE_WEB_CLIENT_ID).",
        )
    try:
        idinfo = google_id_token.verify_oauth2_token(
            request.id_token,
            google_requests.Request(),
            settings.google_web_client_id,
        )
    except ValueError:
        raise HTTPException(
            status_code=401,
            detail="Invalid or expired Google credential",
        ) from None

    if idinfo.get("iss") not in ("accounts.google.com", "https://accounts.google.com"):
        raise HTTPException(status_code=401, detail="Invalid token issuer")

    if not idinfo.get("email_verified", False):
        raise HTTPException(
            status_code=403,
            detail="Verify your Google account email before signing in",
        )

    email = idinfo["email"].strip().lower()
    sub = idinfo["sub"]

    db = get_db_connection()
    if not db:
        raise HTTPException(status_code=500, detail="Database connection failed")

    cursor = db.cursor(dictionary=True)
    try:
        cursor.execute(
            "SELECT id, email, password_hash, google_sub FROM users WHERE google_sub = %s",
            (sub,),
        )
        row = cursor.fetchone()
        if row:
            token = create_access_token(
                subject_email=row["email"], settings=get_settings()
            )
            return LoginResponse(token=token, message="Login successful")

        cursor.execute(
            "SELECT id, email, password_hash, google_sub FROM users WHERE email = %s",
            (email,),
        )
        row = cursor.fetchone()
        if row:
            if row["google_sub"] and row["google_sub"] != sub:
                raise HTTPException(
                    status_code=409,
                    detail="This email is linked to a different Google account.",
                )
            if row["password_hash"]:
                raise HTTPException(
                    status_code=409,
                    detail="This email is registered with a password. Use email sign-in instead.",
                )
            cursor.execute(
                "UPDATE users SET google_sub = %s WHERE id = %s",
                (sub, row["id"]),
            )
            db.commit()
            token = create_access_token(subject_email=email, settings=get_settings())
            return LoginResponse(token=token, message="Login successful")

        try:
            cursor.execute(
                "INSERT INTO users (email, password_hash, google_sub) VALUES (%s, NULL, %s)",
                (email, sub),
            )
            db.commit()
        except mysql.connector.IntegrityError:
            db.rollback()
            raise HTTPException(
                status_code=409,
                detail="Could not create account (email may already be in use).",
            ) from None

        token = create_access_token(subject_email=email, settings=get_settings())
        return LoginResponse(token=token, message="Registration successful")
    finally:
        cursor.close()
        db.close()


@router.post("/login", response_model=LoginResponse)
def login(request: LoginRequest):
    db = get_db_connection()
    if not db:
        raise HTTPException(status_code=500, detail="Database connection failed")

    email = request.email.strip().lower()
    cursor = db.cursor(dictionary=True)
    cursor.execute(
        "SELECT password_hash FROM users WHERE email = %s",
        (email,),
    )
    row = cursor.fetchone()
    cursor.close()
    db.close()

    stored_hash = row["password_hash"] if row else None
    if not row or not stored_hash or not verify_password(request.password, stored_hash):
        raise HTTPException(status_code=401, detail="Invalid email or password")

    token = create_access_token(subject_email=email, settings=get_settings())
    return LoginResponse(token=token, message="Login successful")


@router.post("/register", response_model=LoginResponse)
def register(request: RegisterRequest):
    db = get_db_connection()
    if not db:
        raise HTTPException(status_code=500, detail="Database connection failed")

    email = str(request.email).strip().lower()
    password_hash = hash_password(request.password)
    cursor = db.cursor()
    try:
        try:
            cursor.execute(
                "INSERT INTO users (email, password_hash, google_sub) VALUES (%s, %s, NULL)",
                (email, password_hash),
            )
        except mysql.connector.Error as insert_err:
            # 1054 = unknown column — legacy table without google_sub
            if getattr(insert_err, "errno", None) != 1054:
                raise insert_err
            db.rollback()
            cursor.execute(
                "INSERT INTO users (email, password_hash) VALUES (%s, %s)",
                (email, password_hash),
            )
        db.commit()
    except mysql.connector.IntegrityError:
        db.rollback()
        raise HTTPException(
            status_code=409,
            detail="An account with this email already exists",
        ) from None
    except mysql.connector.Error as e:
        db.rollback()
        raise HTTPException(
            status_code=500,
            detail=(
                f"Database error: {e!s}. "
                "Apply backend/sql/migration_add_google_auth.sql (adds google_sub) for full sign-up, "
                "including Google Sign-In."
            ),
        ) from e
    finally:
        cursor.close()
        db.close()

    token = create_access_token(subject_email=email, settings=get_settings())
    return LoginResponse(token=token, message="Registration successful")
