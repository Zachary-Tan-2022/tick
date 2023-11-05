import { axiosInstance } from "../api/axios";

export const getEvent = async (event_id) => {
    return axiosInstance.get(`/event/${event_id}`).then((res) => res.data);
}

export const getEvents = async () => {
    return axiosInstance.get(`/event`).then((res) => res.data);
}

export const getRecommendedEvents = async (artist_name) => {
    return axiosInstance.get(`/event/recommend/${artist_name}`).then((res) => res.data);
}