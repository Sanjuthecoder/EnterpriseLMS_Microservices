import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import CoursePlayer from '../CoursePlayer';

// Mock dependencies
jest.mock('../../../services/api', () => ({
  __esModule: true,
  default: {
    get: jest.fn().mockResolvedValue({
      data: {
        courseTitle: 'Mock Course',
        progressPercentage: 50,
        lessons: [
          { lessonId: 1, lessonTitle: 'Mock Lesson', moduleTitle: 'Mock Module', contentType: 'Video' }
        ]
      }
    }),
    post: jest.fn().mockResolvedValue({})
  }
}));

jest.mock('../../../contexts/AuthContext', () => ({
  useAuth: () => ({
    user: { username: 'testuser' }
  })
}));

jest.mock('sockjs-client/dist/sockjs', () => {
  return jest.fn().mockImplementation(() => ({
    close: jest.fn(),
  }));
});

jest.mock('@stomp/stompjs', () => ({
  Client: jest.fn().mockImplementation(() => ({
    activate: jest.fn(),
    deactivate: jest.fn(),
    subscribe: jest.fn(),
    publish: jest.fn(),
  }))
}));

describe('CoursePlayer WebSocket & UI integration', () => {
  test('renders CoursePlayer and loads mock data', async () => {
    render(
      <MemoryRouter initialEntries={['/employee/courses/10']}>
        <Routes>
          <Route path="/employee/courses/:courseId" element={<CoursePlayer />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Loading Course Portal...')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Mock Course')).toBeInTheDocument();
      expect(screen.getByText('Mock Module')).toBeInTheDocument();
      expect(screen.getAllByText('Mock Lesson').length).toBeGreaterThan(0);
      // Chat button should be present
      expect(screen.getByRole('button', { name: /Chat/i })).toBeInTheDocument();
    });
  });
});
