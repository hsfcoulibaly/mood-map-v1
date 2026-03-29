import mysql.connector
from mysql.connector.errors import Error as MySQLError

from core.config import get_settings


def get_db_connection():
    s = get_settings()
    try:
        if s.cloud_sql_connection_name:
            # Cloud Run: attach the instance on the service, then use the Unix socket.
            socket_path = f"/cloudsql/{s.cloud_sql_connection_name}"
            return mysql.connector.connect(
                unix_socket=socket_path,
                user=s.db_user,
                password=s.db_password,
                database=s.db_name,
            )
        return mysql.connector.connect(
            host=s.db_host,
            user=s.db_user,
            password=s.db_password,
            database=s.db_name,
        )
    except MySQLError:
        return None
