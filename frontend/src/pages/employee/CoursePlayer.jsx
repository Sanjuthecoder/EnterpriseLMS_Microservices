import React, { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import Modal from '../../components/shared/Modal';
import api from '../../services/api';
import { CheckCircle, PlayCircle, FileText, CheckSquare, ArrowLeft, BookOpen, MessageSquare, X, Send } from 'lucide-react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client/dist/sockjs';

const CoursePlayer = () => {
    const { courseId } = useParams();
    const navigate = useNavigate();
    const { user } = useAuth();

    const [courseData, setCourseData] = useState(null);
    const [activeLessonId, setActiveLessonId] = useState(null);
    const [completedLessons, setCompletedLessons] = useState([]);
    const [progressPercentage, setProgressPercentage] = useState(0);

    // Chat State
    const [isChatOpen, setIsChatOpen] = useState(false);
    const [messages, setMessages] = useState([]);
    const [newMessage, setNewMessage] = useState('');
    const stompClientRef = useRef(null);
    const messagesEndRef = useRef(null);

    // Auto-scroll chat
    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages]);

    // WebSocket Connection and Chat History
    useEffect(() => {
        const companyId = user?.companyId || user?.orgId;
        if (!companyId) return;

        // Fetch Chat History
        const fetchHistory = async () => {
            try {
                // Using 8085 directly to reach the communication service
                const res = await fetch(`http://localhost:8085/api/v1/chat/history/${companyId}/${courseId}`);
                if (res.ok) {
                    const data = await res.json();
                    setMessages(data);
                }
            } catch (error) {
                console.error('Failed to load chat history:', error);
            }
        };
        fetchHistory();

        const socket = new SockJS('http://localhost:8085/api/ws-chat');
        const client = new Client({
            webSocketFactory: () => socket,
            reconnectDelay: 5000,
            onConnect: () => {
                console.log('Connected to Live Chat');
                client.subscribe(`/topic/company/${companyId}/course/${courseId}`, (message) => {
                    if (message.body) {
                        setMessages((prev) => [...prev, JSON.parse(message.body)]);
                    }
                });
            }
        });

        client.activate();
        stompClientRef.current = client;

        return () => {
            if (stompClientRef.current) {
                stompClientRef.current.deactivate();
            }
        };
    }, [courseId, user]);

    const handleSendMessage = (e) => {
        e.preventDefault();
        const companyId = user?.companyId || user?.orgId;
        if (newMessage.trim() && stompClientRef.current?.connected && companyId) {
            stompClientRef.current.publish({
                destination: `/app/chat.send/${companyId}/${courseId}`,
                body: JSON.stringify({
                    sender: user?.username || user?.email || 'Employee',
                    content: newMessage
                })
            });
            setNewMessage('');
        }
    };

    // The lessonGatingMap dictates if a lesson is RECOMMENDED or OPTIONAL
    const [lessonGatingMap, setLessonGatingMap] = useState({});

    useEffect(() => {
        // Fetch Course Content & Gating Map
        const loadCourse = async () => {
            try {
                const res = await api.get(`/employees/courses/${courseId}/content`);

                // Group lessons by moduleTitle
                const modulesMap = {};
                res.data.lessons.forEach(lesson => {
                    const modTitle = lesson.moduleTitle || "General";
                    if (!modulesMap[modTitle]) {
                        modulesMap[modTitle] = { title: modTitle, lessons: [] };
                    }
                    modulesMap[modTitle].lessons.push(lesson);
                });

                const formattedCourse = {
                    courseTitle: res.data.courseTitle,
                    modules: Object.values(modulesMap)
                };

                const gatingMap = {};
                res.data.lessons.forEach(lesson => {
                    if (lesson.gatingStatus) {
                        gatingMap[lesson.lessonId] = lesson.gatingStatus;
                    }
                });

                setCourseData(formattedCourse);
                setLessonGatingMap(gatingMap);

                const allLessons = res.data.lessons;
                const completed = res.data.completedLessons || [];
                setCompletedLessons(completed);

                setProgressPercentage(res.data.progressPercentage || 0);

                // Find first uncompleted lesson
                let firstUncompleted = null;
                for (let l of allLessons) {
                    if (!completed.includes(l.lessonId)) {
                        firstUncompleted = l.lessonId;
                        break;
                    }
                }

                if (firstUncompleted) {
                    setActiveLessonId(firstUncompleted);
                } else if (formattedCourse.modules[0]?.lessons[0]) {
                    setActiveLessonId(formattedCourse.modules[0].lessons[0].lessonId);
                }

            } catch (err) {
                console.error("Failed to load course", err);
            }
        };

        loadCourse();
    }, [courseId]);

    const activeLesson = courseData?.modules.flatMap(m => m.lessons).find(l => l.lessonId === activeLessonId);

    // Keep a ref to the active lesson so the cleanup/save telemetry can access it reliably
    const activeLessonRef = useRef(null);
    useEffect(() => {
        activeLessonRef.current = activeLesson;
    }, [activeLesson]);

    // Video Telemetry Tracking
    const [videoEvents, setVideoEvents] = useState([]);
    const videoEventsRef = useRef([]);
    const totalSeeksRef = useRef(0);
    const lastTimeRef = useRef(0);
    const preSeekTimeRef = useRef(0);
    const highSpeedSecsRef = useRef(0);
    const videoRef = useRef(null);
    const sessionIdRef = useRef(Math.random().toString(36).substring(2, 15));
    const videoDurationRef = useRef(0);

    const markLessonComplete = async (lessonId) => {
        try {
            const res = await api.post(`/employees/courses/${courseId}/lessons/${lessonId}/complete`);
            setCompletedLessons(res.data.completedLessons || []);
            setProgressPercentage(res.data.progressPercentage || 0);
            console.log("Lesson marked complete in DB", lessonId, "New progress:", res.data.progressPercentage);
        } catch (e) {
            console.error("Failed to mark lesson complete in DB", e);
            if (!completedLessons.includes(lessonId)) {
                setCompletedLessons(prev => [...prev, lessonId]);
            }
        }
    };


    const saveTelemetry = async (isCompleted = false) => {
        const lesson = activeLessonRef.current;
        if (!lesson) return;
        const isVideo = lesson.contentType?.toLowerCase() === 'video' || lesson.lessonType?.toLowerCase() === 'video';

        // Save telemetry if it's a video and there are recorded events
        if (isVideo && videoEventsRef.current.length > 0) {
            const duration = videoDurationRef.current;
            const payload = {
                courseId,
                lessonId: lesson.lessonId,
                sessionId: sessionIdRef.current,
                duration,
                totalSeeks: totalSeeksRef.current,
                highSpeedSeconds: highSpeedSecsRef.current,
                completionStatus: isCompleted ? 'completed' : 'in-progress',
                completionPercentage: duration > 0 ? Math.min(100, Math.round((lastTimeRef.current / duration) * 100)) : 0,
                events: [...videoEventsRef.current]
            };

            try {
                await api.post('/telemetry/video-sessions', payload);
                console.log("Telemetry saved", payload);
            } catch (e) {
                console.error("Failed to save telemetry", e);
            }
        }
    };

    useEffect(() => {
        if (activeLesson) {
            const isVideo = activeLesson.contentType?.toLowerCase() === 'video' || activeLesson.lessonType?.toLowerCase() === 'video';
            if (!isVideo) {
                markLessonComplete(activeLesson.lessonId);
            }
        }

        // Return cleanup function to save telemetry for the lesson being left
        return () => {
            saveTelemetry(false);
        };
    }, [activeLessonId]);

    // Periodic telemetry save (every 10 seconds) and beforeunload
    useEffect(() => {
        const intervalId = setInterval(() => {
            saveTelemetry(false);
        }, 10000);

        const handleBeforeUnload = () => {
            saveTelemetry(false);
        };

        window.addEventListener('beforeunload', handleBeforeUnload);

        return () => {
            clearInterval(intervalId);
            window.removeEventListener('beforeunload', handleBeforeUnload);
        };
    }, [activeLessonId]);

    // Reset telemetry variables on lesson transition after cleanup has run
    useEffect(() => {

        videoEventsRef.current = [];
        totalSeeksRef.current = 0;
        lastTimeRef.current = 0;
        preSeekTimeRef.current = 0;
        highSpeedSecsRef.current = 0;
        videoDurationRef.current = 0;
        sessionIdRef.current = Math.random().toString(36).substring(2, 15);
        setVideoEvents([]);
    }, [activeLessonId]);

    const handleVideoEvent = (type, time, metadata = {}) => {
        let eventType = type;
        if (type === 'seek') {
            eventType = time > preSeekTimeRef.current ? 'skip_forward' : 'rewind';
        }

        const event = { type: eventType, videoTime: time, createdAt: new Date().toISOString(), ...metadata };
        videoEventsRef.current = [...videoEventsRef.current, event];

        if (type === 'seek') {
            totalSeeksRef.current += 1;
        }
    };

    if (!courseData) {
        return (
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100vh', background: '#090d16', color: 'white' }}>
                <div style={{ fontSize: '1.2rem', fontWeight: 500 }}>Loading Course Portal...</div>
            </div>
        );
    }

    const renderIcon = (type) => {
        const lowerType = type ? type.toLowerCase() : 'video';
        switch (lowerType) {
            case 'video': return <PlayCircle size={18} />;
            case 'pdf': return <FileText size={18} />;
            case 'quiz': return <CheckSquare size={18} />;
            default: return <FileText size={18} />;
        }
    };

    return (
        <section style={{ display: 'flex', flexDirection: 'column', height: '100vh', width: '100vw', overflow: 'hidden', background: '#090d16', color: '#f3f4f6', fontFamily: 'var(--font-family)' }}>

            {/* Premium Immersive Header Bar */}
            <header style={{
                height: '64px',
                background: '#0f172a',
                borderBottom: '1px solid #1e293b',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '0 2rem',
                flexShrink: 0,
                zIndex: 10
            }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
                    <button
                        onClick={() => navigate('/employee')}
                        style={{
                            background: 'rgba(255, 255, 255, 0.05)',
                            border: '1px solid rgba(255, 255, 255, 0.1)',
                            borderRadius: '8px',
                            color: 'white',
                            cursor: 'pointer',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '0.5rem',
                            padding: '0.5rem 1rem',
                            fontSize: '0.9rem',
                            fontWeight: 500,
                            transition: 'all 0.2s'
                        }}
                        onMouseEnter={(e) => e.currentTarget.style.background = 'rgba(255, 255, 255, 0.1)'}
                        onMouseLeave={(e) => e.currentTarget.style.background = 'rgba(255, 255, 255, 0.05)'}
                    >
                        <ArrowLeft size={16} /> Dashboard
                    </button>

                    <div style={{ height: '20px', width: '1px', background: '#334155' }} />

                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                        <BookOpen size={20} style={{ color: 'var(--primary-color)' }} />
                        <span style={{ fontSize: '1.1rem', fontWeight: 600, color: 'white' }}>{courseData.courseTitle}</span>
                    </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '4px' }}>
                        <span style={{ fontSize: '0.75rem', color: '#94a3b8', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>Course Progress</span>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                            <div style={{ width: '120px', height: '6px', background: '#1e293b', borderRadius: '4px', overflow: 'hidden' }}>
                                <div style={{ height: '100%', width: `${progressPercentage}%`, background: '#10b981', transition: 'width 0.5s cubic-bezier(0.4, 0, 0.2, 1)' }} />
                            </div>
                            <span style={{ fontSize: '0.85rem', fontWeight: 700, color: '#10b981', minWidth: '35px', textAlign: 'right' }}>{progressPercentage}%</span>
                        </div>
                    </div>

                    <div style={{ height: '30px', width: '1px', background: '#334155' }} />

                    <span style={{ fontSize: '0.85rem', color: '#94a3b8', background: 'rgba(37, 99, 235, 0.1)', border: '1px solid rgba(37, 99, 235, 0.2)', padding: '4px 10px', borderRadius: '12px', fontWeight: 500 }}>
                        Learning Portal
                    </span>

                    <button
                        onClick={() => setIsChatOpen(!isChatOpen)}
                        style={{
                            background: isChatOpen ? 'var(--primary-color)' : 'rgba(255, 255, 255, 0.05)',
                            border: '1px solid rgba(255, 255, 255, 0.1)',
                            borderRadius: '8px',
                            color: 'white',
                            cursor: 'pointer',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '0.5rem',
                            padding: '0.5rem 1rem',
                            fontSize: '0.9rem',
                            fontWeight: 500,
                            transition: 'all 0.2s',
                            marginLeft: '10px'
                        }}
                    >
                        <MessageSquare size={18} /> Chat
                    </button>
                </div>
            </header>

            {/* Main Content Area */}
            <div style={{ display: 'flex', flexGrow: 1, overflow: 'hidden' }}>

                {/* Sidebar */}
                <aside style={{
                    width: '320px',
                    flexShrink: 0,
                    background: '#0f172a',
                    borderRight: '1px solid #1e293b',
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'space-between',
                    padding: '1.5rem',
                    overflowY: 'auto'
                }}>
                    <div>
                        {courseData.modules.map((mod, i) => (
                            <div key={i} style={{ marginBottom: '1.75rem' }}>
                                <h3 style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase', marginBottom: '0.75rem', letterSpacing: '1px', fontWeight: 700 }}>
                                    {mod.title}
                                </h3>
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                                    {mod.lessons.map(lesson => {
                                        const isDone = completedLessons.includes(lesson.lessonId);
                                        const gatingStatus = lessonGatingMap[lesson.lessonId];
                                        const isActive = activeLessonId === lesson.lessonId;

                                        return (
                                            <div
                                                key={lesson.lessonId}
                                                onClick={() => setActiveLessonId(lesson.lessonId)}
                                                style={{
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    gap: '12px',
                                                    padding: '0.75rem 1rem',
                                                    borderRadius: '8px',
                                                    cursor: 'pointer',
                                                    background: isActive ? 'rgba(37, 99, 235, 0.15)' : 'transparent',
                                                    borderLeft: `4px solid ${isActive ? 'var(--primary-color)' : 'transparent'}`,
                                                    transition: 'all 0.2s',
                                                    border: isActive ? 'none' : '1px solid transparent'
                                                }}
                                                onMouseEnter={(e) => {
                                                    if (!isActive) e.currentTarget.style.background = 'rgba(255, 255, 255, 0.02)';
                                                }}
                                                onMouseLeave={(e) => {
                                                    if (!isActive) e.currentTarget.style.background = 'transparent';
                                                }}
                                            >
                                                <div style={{ color: isDone ? '#10b981' : '#64748b', display: 'flex', alignItems: 'center' }}>
                                                    {isDone ? <CheckCircle size={18} /> : renderIcon(lesson.contentType)}
                                                </div>
                                                <span style={{ fontSize: '0.9rem', fontWeight: isActive ? 600 : 400, color: isActive ? 'white' : '#94a3b8' }}>
                                                    {lesson.lessonTitle}
                                                </span>

                                                {gatingStatus === 'RECOMMENDED' && (
                                                    <span style={{ marginLeft: 'auto', fontSize: '0.65rem', background: '#f59e0b', color: 'white', padding: '2px 8px', borderRadius: '10px', fontWeight: 700 }}>REC</span>
                                                )}
                                                {gatingStatus === 'OPTIONAL' && (
                                                    <span style={{ marginLeft: 'auto', fontSize: '0.65rem', background: '#334155', color: '#94a3b8', padding: '2px 8px', borderRadius: '10px', fontWeight: 700 }}>OPT</span>
                                                )}
                                            </div>
                                        );
                                    })}
                                </div>
                            </div>
                        ))}
                    </div>

                    {/* Post-Quiz Action */}
                    <div style={{ marginTop: '2rem', borderTop: '1px solid #1e293b', paddingTop: '1.25rem' }}>
                        <button
                            onClick={() => navigate(`/employee/courses/${courseId}/post-quiz`)}
                            className="btn btn-primary"
                            disabled={progressPercentage < 100}
                            style={{
                                width: '100%',
                                padding: '0.75rem',
                                fontWeight: 600,
                                fontSize: '0.95rem',
                                opacity: progressPercentage < 100 ? 0.5 : 1,
                                cursor: progressPercentage < 100 ? 'not-allowed' : 'pointer'
                            }}
                        >
                            Take Final Post-Quiz
                        </button>
                        {progressPercentage < 100 && (
                            <p style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '0.5rem', textAlign: 'center' }}>
                                Progress must be 100% to attempt the final quiz (currently {progressPercentage}%)
                            </p>
                        )}
                    </div>
                </aside>

                {/* Main Content (Immersive Player) */}
                <main style={{ flexGrow: 1, display: 'flex', flexDirection: 'column', background: '#020617', overflow: 'hidden' }}>
                    {activeLesson ? (() => {
                        let fullUrl = activeLesson.contentUrl || '';
                        if (fullUrl && !fullUrl.startsWith('http')) {
                            if (!fullUrl.startsWith('/')) {
                                fullUrl = '/uploads/' + fullUrl;
                            } else if (!fullUrl.startsWith('/uploads')) {
                                fullUrl = '/uploads' + fullUrl;
                            }
                            fullUrl = `http://localhost:8080${fullUrl}`;
                        }
                        const isVideo = activeLesson.contentType?.toLowerCase() === 'video' || activeLesson.lessonType?.toLowerCase() === 'video';
                        const isPdf = activeLesson.contentType?.toLowerCase() === 'pdf' || activeLesson.lessonType?.toLowerCase() === 'pdf';
                        const isText = activeLesson.contentType?.toLowerCase() === 'text' || activeLesson.lessonType?.toLowerCase() === 'text';

                        return (
                            <>
                                {/* Inner Header Bar for Active Lesson */}
                                <div style={{ padding: '1.25rem 2.5rem', background: '#0f172a', borderBottom: '1px solid #1e293b', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexShrink: 0 }}>
                                    <h2 style={{ fontSize: '1.25rem', fontWeight: 600, margin: 0, color: 'white' }}>{activeLesson.title || activeLesson.lessonTitle}</h2>
                                    <span style={{ fontSize: '0.85rem', color: '#64748b' }}>
                                        Type: {activeLesson.contentType || 'Document'}
                                    </span>
                                </div>

                                {/* Immersive View Area */}
                                <div style={{ flexGrow: 1, display: 'flex', justifyContent: 'center', alignItems: 'center', padding: '0', background: '#030712', overflow: 'hidden', position: 'relative' }}>
                                    {isVideo && (
                                        <video
                                            ref={videoRef}
                                            style={{ width: '100%', height: '100%', objectFit: 'contain', background: 'black' }}
                                            controls
                                            key={fullUrl}
                                            onLoadedMetadata={(e) => videoDurationRef.current = e.target.duration}
                                            onPlay={(e) => handleVideoEvent('play', e.target.currentTime)}
                                            onPause={(e) => {
                                                if (e.target.seeking) return;
                                                handleVideoEvent('pause', e.target.currentTime);
                                            }}
                                            onSeeking={() => preSeekTimeRef.current = lastTimeRef.current}
                                            onSeeked={(e) => handleVideoEvent('seek', e.target.currentTime)}
                                            onEnded={async (e) => {
                                                handleVideoEvent('completed', e.target.currentTime);
                                                await markLessonComplete(activeLesson.lessonId);
                                                await saveTelemetry(true);
                                            }}
                                            onRateChange={(e) => handleVideoEvent('ratechange', e.target.currentTime, { playbackRate: e.target.playbackRate })}
                                            onTimeUpdate={(e) => {
                                                if (!e.target.seeking) {
                                                    const delta = e.target.currentTime - lastTimeRef.current;
                                                    if (delta > 0 && delta < 2 && e.target.playbackRate > 1) {
                                                        highSpeedSecsRef.current += delta;
                                                    }
                                                    lastTimeRef.current = e.target.currentTime;
                                                }
                                            }}
                                        >
                                            <source src={fullUrl} type="video/mp4" />
                                        </video>
                                    )}

                                    {isPdf && (
                                        <iframe
                                            src={fullUrl}
                                            title={activeLesson.title || activeLesson.lessonTitle}
                                            width="100%"
                                            height="100%"
                                            style={{ border: 'none', background: 'white' }}
                                        />
                                    )}

                                    {isText && (
                                        <div style={{ width: '100%', height: '100%', overflowY: 'auto', padding: '3rem 4rem', display: 'flex', justifyContent: 'center' }}>
                                            <div dangerouslySetInnerHTML={{ __html: activeLesson.contentHtml || activeLesson.description }} style={{ lineHeight: 1.8, color: '#e2e8f0', maxWidth: '800px', fontSize: '1.15rem' }} />
                                        </div>
                                    )}
                                </div>
                            </>
                        );
                    })() : (
                        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', padding: '2rem', textAlign: 'center', color: '#64748b' }}>
                            <PlayCircle size={64} style={{ marginBottom: '1.5rem', color: 'var(--primary-color)', opacity: 0.8 }} />
                            <h2 style={{ fontSize: '1.5rem', fontWeight: 600, color: 'white', marginBottom: '0.5rem' }}>Select a lesson</h2>
                            <p style={{ maxWidth: '400px', fontSize: '0.95rem', lineHeight: 1.5 }}>Choose a module and lesson from the sidebar to begin learning.</p>
                        </div>
                    )}
                </main>

                {/* Chat Sidebar */}
                {isChatOpen && (
                    <aside style={{
                        width: '350px',
                        flexShrink: 0,
                        background: '#0f172a',
                        borderLeft: '1px solid #1e293b',
                        display: 'flex',
                        flexDirection: 'column',
                        zIndex: 20
                    }}>
                        <div style={{ padding: '1.25rem', borderBottom: '1px solid #1e293b', display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: '#020617' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                <MessageSquare size={18} style={{ color: 'var(--primary-color)' }} />
                                <h3 style={{ fontSize: '1rem', fontWeight: 600, color: 'white', margin: 0 }}>Live Discussion</h3>
                            </div>
                            <button onClick={() => setIsChatOpen(false)} style={{ background: 'transparent', border: 'none', color: '#94a3b8', cursor: 'pointer' }}>
                                <X size={20} />
                            </button>
                        </div>

                        <div style={{ padding: '0.5rem 1rem', background: 'rgba(30, 41, 59, 0.5)', borderBottom: '1px solid #1e293b', fontSize: '0.75rem', color: '#94a3b8', textAlign: 'center', lineHeight: 1.4 }}>
                            Messages are automatically deleted after 7 days. End-to-end encryption is maintained.
                        </div>

                        <div style={{ flexGrow: 1, overflowY: 'auto', padding: '1rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                            {messages.length === 0 ? (
                                <p style={{ textAlign: 'center', color: '#64748b', fontSize: '0.9rem', marginTop: '2rem' }}>No messages yet. Be the first to say hello!</p>
                            ) : (
                                messages.map((msg, idx) => (
                                    <div key={idx} style={{ display: 'flex', flexDirection: 'column', alignItems: msg.sender === (user?.username || user?.email || 'Employee') ? 'flex-end' : 'flex-start' }}>
                                        <span style={{ fontSize: '0.75rem', color: '#64748b', marginBottom: '2px' }}>{msg.sender}</span>
                                        <div style={{
                                            background: msg.sender === (user?.username || user?.email || 'Employee') ? 'var(--primary-color)' : '#1e293b',
                                            color: 'white',
                                            padding: '8px 12px',
                                            borderRadius: '8px',
                                            fontSize: '0.9rem',
                                            maxWidth: '90%',
                                            wordBreak: 'break-word'
                                        }}>
                                            {msg.content}
                                        </div>
                                    </div>
                                ))
                            )}
                            <div ref={messagesEndRef} />
                        </div>

                        <form onSubmit={handleSendMessage} style={{ padding: '1rem', borderTop: '1px solid #1e293b', display: 'flex', gap: '0.5rem', background: '#020617' }}>
                            <input
                                type="text"
                                value={newMessage}
                                onChange={(e) => setNewMessage(e.target.value)}
                                placeholder="Type a message..."
                                style={{ flexGrow: 1, padding: '0.5rem 1rem', borderRadius: '20px', border: '1px solid #334155', background: '#0f172a', color: 'white', outline: 'none' }}
                            />
                            <button type="submit" style={{ background: 'var(--primary-color)', border: 'none', color: 'white', width: '36px', height: '36px', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>
                                <Send size={16} />
                            </button>
                        </form>
                    </aside>
                )}

            </div>
        </section>
    );
};

export default CoursePlayer;
