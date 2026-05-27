import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { apiGet, apiPost } from '@/lib/api';

describe('api client', () => {
  let fetchMock: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  describe('apiGet', () => {
    it('calls fetch with GET + Accept header, returns parsed JSON', async () => {
      fetchMock.mockResolvedValueOnce(
        new Response(JSON.stringify({ status: 'ok' }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      );

      const result = await apiGet<{ status: string }>('/api/health');

      expect(result).toEqual({ status: 'ok' });
      expect(fetchMock).toHaveBeenCalledWith(
        'http://localhost:8080/api/health',
        expect.objectContaining({
          method: 'GET',
          headers: { Accept: 'application/json' },
        }),
      );
    });

    it('throws ApiError on non-2xx with parsed body', async () => {
      fetchMock.mockResolvedValueOnce(
        new Response(
          JSON.stringify({ code: 'NOT_FOUND', message: 'No such thing' }),
          { status: 404 },
        ),
      );

      await expect(apiGet('/api/missing')).rejects.toMatchObject({
        status: 404,
        body: { code: 'NOT_FOUND', message: 'No such thing' },
      });
    });
  });

  describe('apiPost', () => {
    it('sends JSON body + correct headers, returns parsed JSON', async () => {
      fetchMock.mockResolvedValueOnce(
        new Response(JSON.stringify({ id: 1 }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      );

      const result = await apiPost<{ id: number }, { name: string }>(
        '/api/things',
        { name: 'foo' },
      );

      expect(result).toEqual({ id: 1 });
      expect(fetchMock).toHaveBeenCalledWith(
        'http://localhost:8080/api/things',
        expect.objectContaining({
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Accept: 'application/json',
          },
          body: JSON.stringify({ name: 'foo' }),
        }),
      );
    });

    it('throws ApiError with text body when response is not JSON', async () => {
      fetchMock.mockResolvedValueOnce(
        new Response('internal boom', { status: 500 }),
      );

      await expect(apiPost('/api/things', {})).rejects.toMatchObject({
        name: 'ApiError',
        status: 500,
        body: 'internal boom',
      });
    });
  });
});
