import mysql.connector
from mysql.connector.errors import Error as MySQLError

from core.config import get_settings


def get_db_connection():
    s = get_settings()
    try:
        return mysql.connector.connect(
            host=s.db_host,
            user=s.db_user,
            password=s.db_password,
            database=s.db_name,
        )
    except MySQLError:
        return None
